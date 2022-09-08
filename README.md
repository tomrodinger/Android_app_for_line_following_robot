# Android_app_for_line_following_robot
This will allow firmware updates of the line following robot. Will download the new firmware from a server and allow the user to upgrade.

## How to build the app

There are 2 ways to build the Android application. Android studio or command line interface with Gradle.

### Android Studio
1. Download and install Android Studio [from here](https://developer.android.com/studio?gclid=Cj0KCQjwguGYBhDRARIsAHgRm48WtC81hXkAvPavQJn6Tv7dhRW1sXzUaDTmugMi4TdxEtv6fpe6AwsaAmZlEALw_wcB&gclsrc=aw.ds).
2. After installing, open Android Studio and open this project there.
3. First time it will take some time until all the files are synced and indexed.
4. To produce an APK file, click on the Terminal tab in the bottom menu.
5. Write **./gradlew installFullRelease** and press enter. That will build the project and create a release APK file that can be distributed.
6. When the build is done, an APK should be located in **/app/build/outputs/apk/full/release/**.
> If you want to quickly install and test the app , you can connect an Android device to your machine and in Android Studio click on Run>Run 'app' from the menu bar. Alternatively you can click on the green play button.

### CLI Gradle
1. Donwload and install Gradle [from here](https://gradle.org/install/).
2. Open a command line tool and navigate to project root directory.
3. Write **./gradlew installFullRelease** and press enter. That will build the project and create a release APK file that can be distributed.
4. When the build is done, an APK should be located in **/app/build/outputs/apk/full/release/**.

## How to create a new version of the app
1. Open the file **/app/build.gradle**.
2. Locate the versionName String key.
3. Change the value of versionName to newer version. At the time of writing this readme, the version was 1.5.
4. Build the app according to [How to build the app].