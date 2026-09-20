# Camera Coach Android

Ứng dụng Android native hỗ trợ người mới chụp ảnh/quay video.

## V1 Camera Core
- Camera preview trực tiếp
- Camera trước/sau
- Chạm lấy nét
- Pinch zoom
- Torch/flash hỗ trợ
- Chụp ảnh vào Pictures/CameraCoach
- Quay video vào Movies/CameraCoach
- Thu âm khi được cấp quyền
- GitHub Actions tự build APK debug

## Build
GitHub Actions chạy tự động khi push vào main. APK nằm trong artifact `camera-coach-debug-apk`.

## Lộ trình
V1 Camera Core -> Sensor Engine -> Composition AI -> Camera Coach -> Auto Capture -> Auto Enhance.
