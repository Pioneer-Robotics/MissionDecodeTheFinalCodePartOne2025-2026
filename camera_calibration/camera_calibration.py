import cv2
import numpy as np
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

# Open webcam - Use C270 HD WEBCAM (camera index 2)
print("Opening C270 HD WEBCAM...")
camera_index = 2
cap = cv2.VideoCapture(camera_index)

if cap.isOpened():
    # Try to read a frame to verify it's working
    ret, frame = cap.read()
    if not ret:
        print("Error: Could not read from C270 webcam")
        cap.release()
        exit()
    print(f"✓ Successfully opened C270 HD WEBCAM at index {camera_index}")
else:
    print("\nError: Could not open C270 HD WEBCAM at index 2")
    print("Available video devices on your system:")
    print("  - /dev/video0 (HD Webcam - built-in)")
    print("  - /dev/video2 (C270 HD WEBCAM)")
    cap.release()
    exit()

print(f"\nUsing camera at index {camera_index}")

print("\n=== Live Camera Calibration ===")
print("Press SPACE to capture an image when pattern is detected")
print("Press 'c' to start calibration (need at least 10 images)")
print("Press 'q' to quit")
print(f"Pattern type: {pattern_type}")
print(f"Chessboard size: {chessboard_size}")
print("\n")

captured_count = 0
frame_count = 0
last_capture_frame = -30  # To prevent capturing too quickly

while True:
    ret, frame = cap.read()
    if not ret:
        print("Error: Failed to read frame")
        break
    
    frame_count += 1
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    display_frame = frame.copy()
    pattern_found = False
    
    if pattern_type == "chessboard":
        ret, corners = cv2.findChessboardCorners(gray, chessboard_size, None)
        if ret:
            pattern_found = True
            corners2 = cv2.cornerSubPix(gray, corners, (11, 11), (-1, -1), criteria)
            cv2.drawChessboardCorners(display_frame, chessboard_size, corners2, ret)
            
    elif pattern_type == "charuco":
        corners, ids, rejected = cv2.aruco.detectMarkers(gray, charuco_dict)
        if len(corners) > 0:
            ret, charuco_corners, charuco_ids = cv2.aruco.interpolateCornersCharuco(corners, ids, gray, charuco_board)
            if ret and ret > 4:
                pattern_found = True
                cv2.aruco.drawDetectedMarkers(display_frame, corners)
                if charuco_corners is not None:
                    cv2.aruco.drawDetectedCornersCharuco(display_frame, charuco_corners, charuco_ids)
    
    # Display status
    status_text = f"Captured: {captured_count} | Pattern: {'FOUND' if pattern_found else 'NOT FOUND'}"
    cv2.putText(display_frame, status_text, (10, 30), 
                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0) if pattern_found else (0, 0, 255), 2)
    cv2.putText(display_frame, "SPACE: Capture | C: Calibrate | Q: Quit", (10, 60), 
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 1)
    
    cv2.imshow('Camera Calibration', display_frame)
    
    key = cv2.waitKey(1) & 0xFF
    
    # Capture image with SPACE
    if key == ord(' ') and pattern_found and (frame_count - last_capture_frame) > 30:
        if pattern_type == "chessboard":
            objpoints.append(objp)
            imgpoints.append(corners2)
        elif pattern_type == "charuco":
            imgpoints.append(charuco_corners)
            objpoints.append(charuco_board.getChessboardCorners())
        
        captured_count += 1
        last_capture_frame = frame_count
        print(f"Image {captured_count} captured!")
        
        # Flash effect
        cv2.imshow('Camera Calibration', np.ones_like(display_frame) * 255)
        cv2.waitKey(100)
    
    # Start calibration with 'c'
    elif key == ord('c'):
        if captured_count >= 10:
            print("\nStarting calibration...")
            break
        else:
            print(f"Need at least 10 images. Currently have {captured_count}.")
    
    # Quit with 'q'
    elif key == ord('q'):
        print("Exiting without calibration...")
        cap.release()
        cv2.destroyAllWindows()
        exit()

cap.release()
cv2.destroyAllWindows()

if len(objpoints) > 0:
    print(f"\nPerforming calibration with {captured_count} images...")
    img_shape = gray.shape[::-1]
    
    if pattern_type == "chessboard":
        # Camera calibration for chessboard
        ret, camera_matrix, dist_coeffs, rvecs, tvecs = cv2.calibrateCamera(
            objpoints, imgpoints, img_shape, None, None)
    else:
        # Camera calibration for Charuco
        ret, camera_matrix, dist_coeffs, rvecs, tvecs = cv2.aruco.calibrateCameraCharuco(
            charucoCorners=imgpoints,
            charucoIds=[None] * len(imgpoints),  # Updated for new API
            board=charuco_board,
            imageSize=img_shape,
            cameraMatrix=None,
            distCoeffs=None)

    print("\n=== Calibration Results ===")
    print(f"Calibration RMS error: {ret}")
    print("\nCamera matrix (intrinsics):")
    print(camera_matrix)
    print("\nDistortion coefficients:")
    print(dist_coeffs)
    
    # Save calibration results
    np.savez('camera_calibration.npz', 
             camera_matrix=camera_matrix, 
             dist_coeffs=dist_coeffs,
             rms_error=ret)
    print("\nCalibration data saved to 'camera_calibration.npz'")

else:
    print("No calibration data captured.")
