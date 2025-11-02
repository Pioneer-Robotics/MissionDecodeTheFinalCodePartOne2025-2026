#!/bin/bash

# Function to check JDK version
check_jdk_version() {
  local version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
  if [[ $version == 17* ]]; then
    echo "JDK version is 17. Proceeding..."
  else
    echo "Error: JDK version is not 17. Please set JDK 17 as the default."
    exit 1
  fi
}

# Function to build the project using Gradle
build_project() {
  echo "Building the project with Gradle..."
  ./gradlew build
  if [ $? -ne 0 ]; then
    echo "Error: Gradle build failed."
    exit 1
  fi
}

# Function to deploy APK using adb
deploy_apk() {
  echo "Deploying APK to connected device..."
  local apk_path="FtcRobotController/build/outputs/apk/debug/FtcRobotController-debug.apk"
  if [ ! -f "$apk_path" ]; then
    echo "Error: APK not found at $apk_path."
    exit 1
  fi

  adb devices | grep -q "device"
  if [ $? -ne 0 ]; then
    echo "Error: No connected devices found."
    exit 1
  fi

  adb install -r "$apk_path"
  if [ $? -ne 0 ]; then
    echo "Error: Failed to install APK."
    exit 1
  fi

  echo "APK successfully deployed."
}

# Main script execution
check_jdk_version
build_project
deploy_apk