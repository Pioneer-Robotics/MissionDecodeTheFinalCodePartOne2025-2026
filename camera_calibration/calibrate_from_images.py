import cv2
import numpy as np
import glob
import os

# Settings: Choose either "chessboard" or "charuco"
pattern_type = "chessboard"  
# For chessboard, internal corners
chessboard_size = (9, 6)

# For Charuco Board (if chosen)
charuco_dict = cv2.aruco.getPredefinedDictionary(cv2.aruco.DICT_5X5_1000)
charuco_board = cv2.aruco.CharucoBoard((9, 6), squareLength=0.04, markerLength=0.02, dictionary=charuco_dict)

# Termination criteria for corner refinement
criteria = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 30, 0.001)

# Lists to store detected points
objpoints = []  # 3D points in real world space
imgpoints = []  # 2D points in image plane

# Prepare chessboard object points (0,0,0), (1,0,0), ...
objp = np.zeros((chessboard_size[0]*chessboard_size[1], 3), np.float32)
objp[:, :2] = np.mgrid[0:chessboard_size[0], 0:chessboard_size[1]].T.reshape(-1, 2)

# Load calibration images from c270_images folder
image_folder = 'c270_images'
print(f"\n=== Camera Calibration from Images ===")
print(f"Looking for images in '{image_folder}/' folder...")

if not os.path.exists(image_folder):
    print(f"Error: Folder '{image_folder}/' does not exist")
    print(f"Please create the folder and add calibration images")
    exit()

# Support multiple image formats
image_patterns = [
    os.path.join(image_folder, '*.jpg'),
    os.path.join(image_folder, '*.jpeg'),
    os.path.join(image_folder, '*.png'),
    os.path.join(image_folder, '*.bmp')
]

images = []
for pattern in image_patterns:
    images.extend(glob.glob(pattern))

if len(images) == 0:
    print(f"Error: No images found in '{image_folder}/' folder")
    print("Supported formats: .jpg, .jpeg, .png, .bmp")
    exit()

print(f"Found {len(images)} image(s)")
print(f"Pattern type: {pattern_type}")
print(f"Chessboard size: {chessboard_size}")
print()

successful_images = 0
failed_images = 0
img_shape = None

for idx, fname in enumerate(images, 1):
    print(f"Processing image {idx}/{len(images)}: {os.path.basename(fname)}...", end=' ')
    
    img = cv2.imread(fname)
    if img is None:
        print("❌ Failed to read")
        failed_images += 1
        continue
    
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    if img_shape is None:
        img_shape = gray.shape[::-1]
    
    pattern_found = False
    
    if pattern_type == "chessboard":
        ret, corners = cv2.findChessboardCorners(gray, chessboard_size, None)
        if ret:
            pattern_found = True
            objpoints.append(objp)
            corners2 = cv2.cornerSubPix(gray, corners, (11, 11), (-1, -1), criteria)
            imgpoints.append(corners2)
            
            # Optional: Draw and save visualization
            # cv2.drawChessboardCorners(img, chessboard_size, corners2, ret)
            # cv2.imwrite(f'detected_{os.path.basename(fname)}', img)

    elif pattern_type == "charuco":
        corners, ids, rejected = cv2.aruco.detectMarkers(gray, charuco_dict)
        if len(corners) > 0:
            ret, charuco_corners, charuco_ids = cv2.aruco.interpolateCornersCharuco(
                corners, ids, gray, charuco_board)
            if ret and ret > 4:
                pattern_found = True
                imgpoints.append(charuco_corners)
                objpoints.append(charuco_board.getChessboardCorners())
                
                # Optional: Draw and save visualization
                # cv2.aruco.drawDetectedMarkers(img, corners)
                # if charuco_corners is not None:
                #     cv2.aruco.drawDetectedCornersCharuco(img, charuco_corners, charuco_ids)
                # cv2.imwrite(f'detected_{os.path.basename(fname)}', img)
    
    if pattern_found:
        print("✓ Pattern detected")
        successful_images += 1
    else:
        print("❌ Pattern not found")
        failed_images += 1

print()
print(f"Results: {successful_images} successful, {failed_images} failed")

if len(objpoints) < 10:
    print(f"\n⚠ Warning: Only {len(objpoints)} images with detected patterns.")
    print("  For good calibration, 10+ images are recommended.")
    if len(objpoints) < 3:
        print("  Need at least 3 images to perform calibration.")
        exit()

if len(objpoints) > 0:
    print(f"\nPerforming calibration with {len(objpoints)} images...")
    
    if pattern_type == "chessboard":
        # Camera calibration for chessboard
        ret, camera_matrix, dist_coeffs, rvecs, tvecs = cv2.calibrateCamera(
            objpoints, imgpoints, img_shape, None, None)
    else:
        # Camera calibration for Charuco
        ret, camera_matrix, dist_coeffs, rvecs, tvecs = cv2.aruco.calibrateCameraCharuco(
            charucoCorners=imgpoints,
            charucoIds=[None] * len(imgpoints),
            board=charuco_board,
            imageSize=img_shape,
            cameraMatrix=None,
            distCoeffs=None)

    print("\n=== Calibration Results ===")
    print(f"Calibration RMS error: {ret}")
    
    # Quality assessment
    if ret < 0.5:
        print("Quality: ✓ Excellent (< 0.5 pixels)")
    elif ret < 1.0:
        print("Quality: ✓ Good (< 1.0 pixel)")
    elif ret < 2.0:
        print("Quality: ⚠ Acceptable (< 2.0 pixels)")
    else:
        print("Quality: ✗ Poor (≥ 2.0 pixels) - Consider recalibrating")
    
    print("\nCamera matrix (intrinsics):")
    print(camera_matrix)
    print("\nDistortion coefficients:")
    print(dist_coeffs)
    
    # Save calibration results
    np.savez('camera_calibration_c270.npz', 
             camera_matrix=camera_matrix, 
             dist_coeffs=dist_coeffs,
             rms_error=ret,
             image_count=len(objpoints))
    print("\n✓ Calibration data saved to 'camera_calibration_c270.npz'")
    
    # Also save as default name for compatibility
    np.savez('camera_calibration.npz', 
             camera_matrix=camera_matrix, 
             dist_coeffs=dist_coeffs,
             rms_error=ret,
             image_count=len(objpoints))
    print("✓ Calibration data also saved to 'camera_calibration.npz'")

else:
    print("\nNo calibration pattern detected in any images.")
