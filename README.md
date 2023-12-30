# SPEN To PC Android App Developer Guide

## Introduction

The SPEN To PC Android App is a Kotlin-based application designed for remote control of a desktop computer using SPen Air Actions. This technical overview provides insights into special components, development setup, prerequisites, and acknowledgments.

**For the Complete Guide:**  [SPEN To PC Wiki](https://github.com/th3-s7r4ng3r/SPEN-To-PC-WindowsApp/wiki)

## Components and Features

**Networking Components**
The app relies on networking components for communication:

 - **Socket:** Establishes a persistent connection between the Android
   device and the desktop.
 - **OutputStream:** Enables the transmission of data streams to the
   connected desktop.

**Coroutines for Asynchronous Operations**
The app utilizes Kotlin coroutines to manage asynchronous operations efficiently. This ensures responsive behavior and optimal resource utilization.


## Developer Guide

**IDE and Language**

 - **Language:** Kotlin
 - **IDE:** Android Studio (With Android SDK 29 or higher)

 **Dependencies**
The app utilizes external libraries for enhanced functionality:

 - **Jared Rummler's DeviceName:** Retrieves the device name dynamically.
- **Coroutine:** DelicateCoroutinesApi is used for asynchronous operations.

## Contact and Support

For inquiries, feedback, or support, developers can reach out through the following channels: **Email:** [th3.s7r4ng3r@gmail.com](mailto:th3.s7r4ng3r@gmail.com)


## Acknowledgment
The development of the SPEN To PC Android App acknowledges the [Android Open Source Project (AOSP)](https://source.android.com/) for providing the platform. Also, [Samsung Developers Code Lab](https://developer.samsung.com/codelab) for providing materials to implement Air Actions, enabling SPen gestures for 3rd party apps. Additionally, [Samsung Remote Test Lab (RTL)](https://developer.samsung.com/remote-test-lab) is used for testing the app compatibility on a wide variety of Galaxy Devices.
