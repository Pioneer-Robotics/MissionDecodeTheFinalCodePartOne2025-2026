#!/usr/bin/env python3
"""
FTC Test Matrix Automation - CSV to ODS Converter
Converts test log CSV files to ODS format and updates the test matrix spreadsheet
"""

import pandas as pd
import numpy as np
from odf.opendocument import OpenDocumentSpreadsheet, load
from odf.table import Table, TableRow, TableCell
from odf.text import P
from odf.style import Style, TableColumnProperties, TableRowProperties, TableCellProperties
from odf.number import NumberStyle, Number, Text as NumberText
import glob
import os
from datetime import datetime
from pathlib import Path

class TestMatrixUpdater:
    def __init__(self, matrix_file_path):
        """Initialize with path to existing test matrix ODS file"""
        self.matrix_file = Path(matrix_file_path)
        if not self.matrix_file.exists():
            raise FileNotFoundError(f"Test matrix not found: {matrix_file_path}")
        
        self.doc = load(str(self.matrix_file))
        self.spreadsheet = self.doc.spreadsheet
        
        # Find sheets
        self.sheets = {}
        for table in self.spreadsheet.getElementsByType(Table):
            name = table.getAttribute('name')
            self.sheets[name] = table
        
        print(f"📊 Loaded test matrix: {self.matrix_file.name}")
        print(f"   Found sheets: {list(self.sheets.keys())}")
    
    def load_test_csv(self, csv_path):
        """Load and parse test CSV file"""
        df = pd.read_csv(csv_path, comment='#')
        
        # Parse test summary from comments
        summary = {}
        with open(csv_path, 'r') as f:
            for line in f:
                if line.startswith('# '):
                    line = line[2:].strip()
                    if ':' in line:
                        key, value = line.split(':', 1)
                        summary[key.strip()] = value.strip()
        
        return df, summary
    
    def analyze_test_data(self, df, summary):
        """Analyze test data and generate results"""
        results = {
            'test_id': summary.get('Test ID', 'UNKNOWN'),
            'description': summary.get('Description', ''),
            'test_type': summary.get('Type', 'FUNCTIONAL'),
            'result': summary.get('Result', 'NOT_TESTED'),
            'duration': float(summary.get('Duration', '0').split()[0]),
            'battery_start': float(summary.get('Battery', '0').split('→')[0].strip().rstrip('V')),
            'battery_end': float(summary.get('Battery', '0').split('→')[1].split('(')[0].strip().rstrip('V')),
            'metrics': {},
            'events': {}
        }
        
        # Analyze metrics
        metrics_df = df[df['metric_name'].notna()]
        for metric_name in metrics_df['metric_name'].unique():
            metric_data = metrics_df[metrics_df['metric_name'] == metric_name]['metric_value']
            results['metrics'][metric_name] = {
                'mean': metric_data.mean(),
                'std': metric_data.std(),
                'min': metric_data.min(),
                'max': metric_data.max(),
                'count': len(metric_data)
            }
        
        # Analyze events
        events_df = df[df['event_type'].notna()]
        event_counts = events_df['event_type'].value_counts().to_dict()
        results['events'] = event_counts
        
        # Calculate specific performance metrics
        if 'SHOT_FIRED' in event_counts:
            shots_df = events_df[events_df['event_type'] == 'SHOT_FIRED']
            # Parse event_params to get hit/miss
            hits = 0
            total = len(shots_df)
            for params in shots_df['event_params']:
                if 'result=HIT' in str(params):
                    hits += 1
            results['shot_accuracy'] = (hits / total * 100) if total > 0 else 0
            results['shots_total'] = total
            results['shots_hit'] = hits
        
        if 'COLLECTION_ATTEMPT' in event_counts:
            collections_df = events_df[events_df['event_type'] == 'COLLECTION_ATTEMPT']
            successes = 0
            total = len(collections_df)
            for params in collections_df['event_params']:
                if 'result=SUCCESS' in str(params):
                    successes += 1
            results['collection_success_rate'] = (successes / total * 100) if total > 0 else 0
            results['collections_total'] = total
            results['collections_success'] = successes
        
        return results
    
    def update_test_matrix_sheet(self, results):
        """Add new row to Test Matrix sheet"""
        if 'Test Matrix' not in self.sheets:
            print("⚠️  Test Matrix sheet not found")
            return
        
        table = self.sheets['Test Matrix']
        
        # Find next empty row
        rows = table.getElementsByType(TableRow)
        next_row_idx = len(rows)
        
        # Create new row
        new_row = TableRow()
        
        # Populate cells
        cells_data = [
            results['test_id'],                    # Test ID
            'Dynamic' if 'auto' in results['test_id'].lower() else 'Static',  # Motion
            'On',                                  # Power
            results['test_type'],                  # Type
            results['description'],                # Objectives
            self._determine_systems(results),      # Systems
            'Software Team',                       # Team
            results['result']                      # Result
        ]
        
        for data in cells_data:
            cell = TableCell()
            p = P(text=str(data))
            cell.addElement(p)
            new_row.addElement(cell)
        
        table.addElement(new_row)
        print(f"✅ Added test to Test Matrix: {results['test_id']}")
    
    def update_characterization_sheet(self, results):
        """Add characterization data"""
        if 'Characterization' not in self.sheets:
            print("⚠️  Characterization sheet not found")
            return
        
        # Only update if it's a characterization test
        if results['test_type'] != 'CHARACTERIZATION':
            return
        
        table = self.sheets['Characterization']
        rows = table.getElementsByType(TableRow)
        
        # Find appropriate row based on battery level
        battery_pct = (results['battery_start'] / 12.0) * 100
        
        # Add row with characterization data
        new_row = TableRow()
        
        # Get flywheel metrics if available
        spin_up_time = results['metrics'].get('plcs_flywheel_spin_up', {}).get('mean', 0)
        velocity = results['metrics'].get('plcs_flywheel_velocity', {}).get('mean', 0)
        
        cells_data = [
            '',                                    # Char ID (manual)
            'Baseline Flywheel',                   # Unit Under Test
            'Stationary',                          # Condition
            f"{battery_pct:.0f}",                  # Battery %
            f"{velocity:.0f}",                     # Speed Command
            '53.3°',                              # Angle (from Constants)
            f"{spin_up_time:.3f}",                # Spin-up Time
            '',                                    # Initial Velocity
            '',                                    # Peak Height
            ''                                     # Range
        ]
        
        for data in cells_data:
            cell = TableCell()
            p = P(text=str(data))
            cell.addElement(p)
            new_row.addElement(cell)
        
        table.addElement(new_row)
        print(f"✅ Added characterization data")
    
    def _determine_systems(self, results):
        """Determine which systems were tested based on metrics"""
        systems = set()
        
        for metric_name in results['metrics'].keys():
            if 'mcs_' in metric_name:
                systems.add('MCS')
            if 'plcs_' in metric_name:
                systems.add('PLCS')
            if 'ams_' in metric_name:
                systems.add('AMS')
        
        if 'SHOT_FIRED' in results['events']:
            systems.add('PLCS')
        if 'COLLECTION_ATTEMPT' in results['events']:
            systems.add('AMS')
        
        return ', '.join(sorted(systems)) if systems else 'All'
    
    def generate_summary_report(self, all_results):
        """Generate text summary of all tests"""
        report = []
        report.append("="*60)
        report.append("FTC DECODE Test Matrix Summary")
        report.append("="*60)
        report.append(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        report.append(f"Total Tests: {len(all_results)}")
        report.append("")
        
        # Count by result
        results_count = {}
        for r in all_results:
            result = r['result']
            results_count[result] = results_count.get(result, 0) + 1
        
        report.append("Test Results:")
        for result, count in sorted(results_count.items()):
            pct = (count / len(all_results) * 100) if all_results else 0
            report.append(f"  {result}: {count} ({pct:.1f}%)")
        
        report.append("")
        report.append("Performance Metrics:")
        
        # Aggregate shot accuracy
        shot_accuracies = [r.get('shot_accuracy', 0) for r in all_results if 'shot_accuracy' in r]
        if shot_accuracies:
            report.append(f"  Shot Accuracy: {np.mean(shot_accuracies):.1f}% (avg)")
            report.append(f"    Best: {max(shot_accuracies):.1f}%")
            report.append(f"    Worst: {min(shot_accuracies):.1f}%")
        
        # Aggregate collection success
        collection_rates = [r.get('collection_success_rate', 0) for r in all_results if 'collection_success_rate' in r]
        if collection_rates:
            report.append(f"  Collection Success: {np.mean(collection_rates):.1f}% (avg)")
            report.append(f"    Best: {max(collection_rates):.1f}%")
            report.append(f"    Worst: {min(collection_rates):.1f}%")
        
        report.append("")
        report.append("Individual Tests:")
        for r in all_results:
            report.append(f"  [{r['result']}] {r['test_id']}: {r['description']}")
            if 'shot_accuracy' in r:
                report.append(f"        Shot accuracy: {r['shot_accuracy']:.1f}%")
            if 'collection_success_rate' in r:
                report.append(f"        Collection: {r['collection_success_rate']:.1f}%")
        
        report.append("="*60)
        return "\n".join(report)
    
    def save(self, output_path=None):
        """Save updated ODS file"""
        if output_path is None:
            output_path = self.matrix_file.parent / f"{self.matrix_file.stem}_updated{self.matrix_file.suffix}"
        
        self.doc.save(str(output_path))
        print(f"💾 Saved updated test matrix: {output_path}")
        return output_path

def process_test_logs(log_dir, matrix_file, output_dir=None):
    """
    Process all test logs in a directory and update the test matrix
    
    Args:
        log_dir: Directory containing test_*.csv files
        matrix_file: Path to FTC test matrix ODS file
        output_dir: Optional output directory for updated matrix
    """
    log_dir = Path(log_dir)
    if output_dir:
        output_dir = Path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)
    
    # Find all test CSV files
    csv_files = sorted(log_dir.glob('test_*.csv'))
    
    if not csv_files:
        print(f"⚠️  No test CSV files found in {log_dir}")
        return
    
    print(f"\n📁 Found {len(csv_files)} test log(s)")
    
    # Load test matrix
    updater = TestMatrixUpdater(matrix_file)
    
    # Process each test
    all_results = []
    for csv_file in csv_files:
        print(f"\n📄 Processing: {csv_file.name}")
        
        try:
            df, summary = updater.load_test_csv(csv_file)
            results = updater.analyze_test_data(df, summary)
            all_results.append(results)
            
            # Update sheets
            updater.update_test_matrix_sheet(results)
            updater.update_characterization_sheet(results)
            
            print(f"   Result: {results['result']}")
            if 'shot_accuracy' in results:
                print(f"   Shot accuracy: {results['shot_accuracy']:.1f}%")
            if 'collection_success_rate' in results:
                print(f"   Collection success: {results['collection_success_rate']:.1f}%")
        
        except Exception as e:
            print(f"   ❌ Error: {e}")
            continue
    
    # Save updated matrix
    output_path = output_dir / matrix_file.name if output_dir else None
    saved_path = updater.save(output_path)
    
    # Generate summary
    summary_report = updater.generate_summary_report(all_results)
    print("\n" + summary_report)
    
    # Save summary to file
    summary_file = saved_path.parent / f"test_summary_{datetime.now().strftime('%Y%m%d_%H%M%S')}.txt"
    with open(summary_file, 'w') as f:
        f.write(summary_report)
    print(f"\n📝 Summary saved to: {summary_file}")
    
    return saved_path, all_results

if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description='Update FTC test matrix from log files')
    parser.add_argument('log_dir', help='Directory containing test_*.csv files')
    parser.add_argument('matrix_file', help='Path to test matrix ODS file')
    parser.add_argument('-o', '--output', help='Output directory for updated matrix')
    
    args = parser.parse_args()
    
    process_test_logs(args.log_dir, args.matrix_file, args.output)
    
    print("\n✅ Test matrix update complete!")

"""
USAGE EXAMPLES:

1. Basic usage:
   python update_test_matrix.py /path/to/test_logs FTC_2025-26_Decode_Requirements_TestMatrix.ods

2. With output directory:
   python update_test_matrix.py ./test_logs ./test_matrix.ods -o ./results

3. From Python:
   from update_test_matrix import process_test_logs
   process_test_logs('./test_logs', './matrix.ods')
"""
