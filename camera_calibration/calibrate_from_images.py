import cv2
import numpy as np
import glob
import os
from tqdm import tqdm

# ============================================================
# Configuration
# ============================================================
TARGET_RESOLUTION = (1280, 720)
PATTERN_TYPE = "chessboard"  # "chessboard" or "charuco"
CHESSBOARD_SIZE = (9, 6)
SQUARE_SIZE_M = 0.025
IMAGE_FOLDER = "c270_images"
MIN_IMAGES_FOR_CALIBRATION = 10

CALIB_FLAGS = cv2.CALIB_RATIONAL_MODEL | cv2.CALIB_ZERO_TANGENT_DIST
CB_FLAGS = cv2.CALIB_CB_ADAPTIVE_THRESH + cv2.CALIB_CB_NORMALIZE_IMAGE
CRITERIA = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 50, 1e-6)

CHARUCO_DICT = cv2.aruco.getPredefinedDictionary(cv2.aruco.DICT_5X5_1000)
CHARUCO_BOARD = cv2.aruco.CharucoBoard(
    CHESSBOARD_SIZE, squareLength=0.04, markerLength=0.02, dictionary=CHARUCO_DICT
)

OBJP = np.zeros((CHESSBOARD_SIZE[0] * CHESSBOARD_SIZE[1], 3), np.float32)
OBJP[:, :2] = np.mgrid[0:CHESSBOARD_SIZE[0], 0:CHESSBOARD_SIZE[1]].T.reshape(-1, 2)
OBJP *= SQUARE_SIZE_M

assert PATTERN_TYPE in {"chessboard", "charuco"}

print("\n=== Camera Calibration ===")
print(f"Images: {IMAGE_FOLDER}")

if not os.path.exists(IMAGE_FOLDER):
    print("Missing image folder")
    raise SystemExit(1)

image_patterns = [os.path.join(IMAGE_FOLDER, ext) for ext in ("*.jpg", "*.jpeg", "*.png", "*.bmp")]
images = [i for pattern in image_patterns for i in glob.glob(pattern)]

if not images:
    print("No images found")
    raise SystemExit(1)

print(f"Found {len(images)} images")

successful, failed = 0, 0
img_shape = None
objpoints, imgpoints = [], []
charuco_corners_list, charuco_ids_list = [], []

# ============================================================
# Image Processing
# ============================================================
for fname in tqdm(images, desc="Processing", unit="img"):
    img = cv2.imread(fname)
    if img is None:
        failed += 1
        continue

    if TARGET_RESOLUTION:
        w, h = TARGET_RESOLUTION
        if (img.shape[1], img.shape[0]) != (w, h):
            img = cv2.resize(img, (w, h), interpolation=cv2.INTER_LINEAR)

    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    if img_shape is None:
        img_shape = gray.shape[::-1]

    found = False

    match PATTERN_TYPE:
        case "chessboard":
            ret, corners = cv2.findChessboardCorners(gray, CHESSBOARD_SIZE, CB_FLAGS)
            if ret:
                found = True
                objpoints.append(OBJP.copy())
                subpix = cv2.cornerSubPix(gray, corners, (11, 11), (-1, -1), CRITERIA)
                imgpoints.append(subpix)

        case "charuco":
            corners, ids, _ = cv2.aruco.detectMarkers(gray, CHARUCO_DICT)
            if corners and ids is not None:
                count, ccorners, cids = cv2.aruco.interpolateCornersCharuco(
                    corners, ids, gray, CHARUCO_BOARD
                )
                if count and count > 4:
                    found = True
                    charuco_corners_list.append(ccorners)
                    charuco_ids_list.append(cids)

    if found:
        successful += 1
    else:
        failed += 1

print(f"Detected: {successful}, Failed: {failed}")

count = len(objpoints) if PATTERN_TYPE == "chessboard" else len(charuco_corners_list)
if count < MIN_IMAGES_FOR_CALIBRATION:
    print(f"Too few usable images ({count})")
    raise SystemExit(1)

# ============================================================
# Reprojection Error
# ============================================================
def reproj_errors(objp, imgp, rvecs, tvecs, K, D):
    errs = []
    for o, i, r, t in zip(objp, imgp, rvecs, tvecs):
        p2, _ = cv2.projectPoints(o, r, t, K, D)
        errs.append(np.sqrt(np.mean(np.sum((i.reshape(-1, 2) - p2.reshape(-1, 2)) ** 2, axis=1))))
    return np.array(errs)

# ============================================================
# Calibration
# ============================================================
match PATTERN_TYPE:
    case "chessboard":
        ret, K, D, rvecs, tvecs = cv2.calibrateCamera(
            objpoints, imgpoints, img_shape, None, None, flags=CALIB_FLAGS, criteria=CRITERIA
        )
        errors = reproj_errors(objpoints, imgpoints, rvecs, tvecs, K, D)

    case "charuco":
        ret, K, D, rvecs, tvecs = cv2.aruco.calibrateCameraCharuco(
            charuco_corners_list, charuco_ids_list, CHARUCO_BOARD, img_shape,
            None, None, flags=CALIB_FLAGS, criteria=CRITERIA
        )
        objpoints = []
        imgpoints = []
        board_pts = CHARUCO_BOARD.getChessboardCorners()
        for cc, ids in zip(charuco_corners_list, charuco_ids_list):
            objpoints.append(board_pts[ids.ravel()])
            imgpoints.append(cc)
        errors = reproj_errors(objpoints, imgpoints, rvecs, tvecs, K, D)

dist_list = [round(x, 4) for x in D.ravel().tolist()[:8]]

# ============================================================
# Output
# ============================================================
print("\nResults:")
print(f"RMS: {ret:.6f}")
print(f"Mean Error: {errors.mean():.4f}")
print(f"StdDev Error: {errors.std():.4f}")
print(f"Camera Matrix:\n{K}")
print(f"Distortion (8): {dist_list}")

print("\n<Camera vid=\"Logitech\" pid=\"0x0825\">")
print("  <Calibration")
print(f"    size=\"{img_shape[0]} {img_shape[1]}\"")
print(f"    focalLength=\"{K[0,0]:.4f}, {K[1,1]:.4f}\"")
print(f"    principalPoint=\"{K[0,2]:.4f}, {K[1,2]:.4f}\"")
print(f"    distortionCoefficients=\"{', '.join(f'{v:.4f}' for v in dist_list)}\"")
print("  />")
print("</Camera>")