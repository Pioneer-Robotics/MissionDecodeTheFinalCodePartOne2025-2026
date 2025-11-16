import numpy as np
import os

def load_calibration(filename='camera_calibration_c270.npz'):
    """Load camera calibration data from file."""
    if not os.path.exists(filename):
        print(f"Error: Calibration file '{filename}' not found.")
        print("Please run camera_calibration.py first to generate calibration data.")
        return None
    
    try:
        data = np.load(filename)
        return data
    except Exception as e:
        print(f"Error loading calibration file: {e}")
        return None


def display_calibration_results(data):
    """Display calibration results in a formatted way."""
    print("\n" + "="*60)
    print("          CAMERA CALIBRATION RESULTS")
    print("="*60 + "\n")
    
    # RMS Error
    if 'rms_error' in data:
        rms_error = data['rms_error']
        print(f"RMS Re-projection Error: {rms_error:.6f} pixels")
        print("\nCalibration Quality Assessment:")
        if rms_error < 0.5:
            print("  ✓ Excellent (< 0.5 pixels)")
        elif rms_error < 1.0:
            print("  ✓ Good (< 1.0 pixel)")
        elif rms_error < 2.0:
            print("  ⚠ Acceptable (< 2.0 pixels)")
        else:
            print("  ✗ Poor (≥ 2.0 pixels) - Consider recalibrating")
        print()
    
    # Camera Matrix (Intrinsic Parameters)
    camera_matrix = data['camera_matrix']
    print("-" * 60)
    print("CAMERA MATRIX (Intrinsic Parameters):")
    print("-" * 60)
    print(camera_matrix)
    print()
    
    # Extract focal lengths and principal point
    fx = camera_matrix[0, 0]
    fy = camera_matrix[1, 1]
    cx = camera_matrix[0, 2]
    cy = camera_matrix[1, 2]
    
    print("Extracted Parameters:")
    print(f"  Focal Length (X): fx = {fx:.2f} pixels")
    print(f"  Focal Length (Y): fy = {fy:.2f} pixels")
    print(f"  Principal Point:  cx = {cx:.2f}, cy = {cy:.2f} pixels")
    print(f"  Aspect Ratio:     fx/fy = {fx/fy:.4f}")
    print()
    
    # Distortion Coefficients
    dist_coeffs = data['dist_coeffs']
    print("-" * 60)
    print("DISTORTION COEFFICIENTS:")
    print("-" * 60)
    print(dist_coeffs)
    print()
    
    # Interpret distortion coefficients
    k1, k2, p1, p2, k3 = dist_coeffs[0][:5] if dist_coeffs.shape[1] >= 5 else (*dist_coeffs[0], 0, 0, 0)[:5]
    
    print("Coefficient Breakdown:")
    print(f"  k1 (radial):     {k1:.6f}")
    print(f"  k2 (radial):     {k2:.6f}")
    print(f"  p1 (tangential): {p1:.6f}")
    print(f"  p2 (tangential): {p2:.6f}")
    print(f"  k3 (radial):     {k3:.6f}")
    print()
    
    # Distortion assessment
    radial_mag = abs(k1) + abs(k2) + abs(k3)
    tangential_mag = abs(p1) + abs(p2)
    
    print("Distortion Assessment:")
    if radial_mag < 0.1:
        print(f"  Radial distortion:     Low (sum = {radial_mag:.4f})")
    elif radial_mag < 0.5:
        print(f"  Radial distortion:     Moderate (sum = {radial_mag:.4f})")
    else:
        print(f"  Radial distortion:     High (sum = {radial_mag:.4f})")
    
    if tangential_mag < 0.01:
        print(f"  Tangential distortion: Low (sum = {tangential_mag:.6f})")
    else:
        print(f"  Tangential distortion: Moderate/High (sum = {tangential_mag:.6f})")
    
    print("\n" + "="*60)
    print()


def export_to_text(data, filename='calibration_results.txt'):
    """Export calibration results to a text file."""
    with open(filename, 'w') as f:
        f.write("CAMERA CALIBRATION RESULTS\n")
        f.write("="*60 + "\n\n")
        
        if 'rms_error' in data:
            f.write(f"RMS Re-projection Error: {data['rms_error']:.6f} pixels\n\n")
        
        f.write("Camera Matrix (Intrinsic Parameters):\n")
        f.write(str(data['camera_matrix']) + "\n\n")
        
        f.write("Distortion Coefficients:\n")
        f.write(str(data['dist_coeffs']) + "\n\n")
        
        # Additional parameters
        camera_matrix = data['camera_matrix']
        fx = camera_matrix[0, 0]
        fy = camera_matrix[1, 1]
        cx = camera_matrix[0, 2]
        cy = camera_matrix[1, 2]
        
        f.write("Extracted Parameters:\n")
        f.write(f"  fx = {fx:.2f}\n")
        f.write(f"  fy = {fy:.2f}\n")
        f.write(f"  cx = {cx:.2f}\n")
        f.write(f"  cy = {cy:.2f}\n")
    
    print(f"✓ Calibration results exported to '{filename}'")


def export_for_opencv(data, filename='camera_params.py'):
    """Export calibration as Python variables for easy import."""
    camera_matrix = data['camera_matrix']
    dist_coeffs = data['dist_coeffs']
    
    with open(filename, 'w') as f:
        f.write("# Camera Calibration Parameters\n")
        f.write("# Generated from camera calibration process\n\n")
        f.write("import numpy as np\n\n")
        
        f.write("# Camera Matrix (Intrinsic Parameters)\n")
        f.write(f"camera_matrix = np.array({camera_matrix.tolist()})\n\n")
        
        f.write("# Distortion Coefficients\n")
        f.write(f"dist_coeffs = np.array({dist_coeffs.tolist()})\n\n")
        
        if 'rms_error' in data:
            f.write(f"# RMS Re-projection Error\n")
            f.write(f"rms_error = {data['rms_error']}\n\n")
        
        f.write("# To use in your code:\n")
        f.write("# from camera_params import camera_matrix, dist_coeffs\n")
        f.write("# undistorted = cv2.undistort(image, camera_matrix, dist_coeffs)\n")
    
    print(f"✓ OpenCV-compatible parameters exported to '{filename}'")


if __name__ == "__main__":
    # Load calibration data
    data = load_calibration('camera_calibration.npz')
    
    if data is not None:
        # Display results
        display_calibration_results(data)
        
        # Export to different formats
        export_to_text(data)
        export_for_opencv(data)
        
        print("\nTo use these calibration results in your code:")
        print("  >>> from camera_params import camera_matrix, dist_coeffs")
        print("  >>> import cv2")
        print("  >>> undistorted_img = cv2.undistort(img, camera_matrix, dist_coeffs)")
