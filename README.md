# Approov Demo

The aim of this demo is to gently introduce you to the client-side components of Approov and show you how to integrate them into a simple app. The scenario is very simple:

    An existing server is running in the cloud; it has 2 end-points.
    You will build an app that connects to the server and query an Approov protected end-point.

Please register for an Approov *Shapes Demo* at [info.approov.io.demo](https://info.approov.io/demo).
You will receive an email with a registration token and a donwload link which contains everything you need to get a client up and running. It consists of:

* This source code for Android and iOS clients (preconfigured with test libraries)
* Registration tools - your registration access token should have been emailed to you when you
    signed up for the demo.
* SDK Libraries for Android and iOS
* This README

# Pre-requisites

The client libraries are for use with Android and iOS.
* To build the Android library you will need Android Studio installed as well as the Android SDK.
* For iOS you will need Xcode.
* You will also need access to the internet to register your app with our servers and to talk to the demo server.
* An Android or iOS device is also needed so you can easily test any apps that you build.

# Testing the Shapes server

The Shapes demo server is really simple, you can access it at https://demo-server.approovr.io/.
It has 2 endpoints:

* A Hello endpoint (https://demo-server.approovr.io/hello) that returns a string
* A Shapes endpoint (https://demo-server.approovr.io/shapes) that returns a random shape

The Hello endpoint has no security (except https) so you should be able to
access it using your software of choice. For the purposes of our examples we
will just use curl.

```
$ curl -D- https://demo-server.approovr.io/hello
HTTP/1.0 200 OK
Content-Type: text/html; charset=utf-8
Content-Length: 12
Server: Werkzeug/0.11.15 Python/3.4.3
Date: Tue, 31 Jan 2017 23:38:52 GMT

Hello World!
```

The Shape endpoint is set up to expect an Approov token. If you try to access
it using curl without the correct header, or with a header that contains an
invalid token, you will get a 400 response.

```
$ curl -D- https://demo-server.approovr.io/shapes
HTTP/1.0 400 BAD REQUEST
Content-Type: text/html
Content-Length: 192
Server: Werkzeug/0.11.15 Python/3.4.3
Date: Tue, 31 Jan 2017 23:43:40 GMT

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 3.2 Final//EN">
<title>400 Bad Request</title>
<h1>Bad Request</h1>
<p>The browser (or proxy) sent a request that this server could not
understand.</p>
```

Now we have checked that the server is up and running and our Shape endpoint is
protected, we can use a valid client to access it properly. But first we have
to build it.

# Building the Android App

In the download there is a 'client/' directory that has all of the code for the
Android and iOS clients. Before building the Android app you will need to ensure
that you have the Android SDK installed.
This can be installed independently or along with AndroidStudio, see
https://developer.android.com/studio/index.html for details.
Once installed, the following commands will build the android app:

```
$ cd clients/android/shapes-client
$ export ANDROID_HOME=<path to installed Android SDK root>
$ ./gradlew assembleDebug
$ ls app/build/outputs/apk # to see the generated APKs
```

Once you have the `.apk` you can install it on a device using adb:

```
$ $ANDROID_HOME/platform-tools/adb install app/build/outputs/apk/app-debug.apk
```

# Building the iOS App

In the download there is a `client/` directory that has all of the code for the
Android and iOS clients. For iOS, an Xcode project is included for the Shapes Demo
which has been tested with version 8.1 of Xcode on macOS Sierra.
To build this project, you will need to ensure that your system is set up with
Xcode 8.1 and can run iOS 8 or above apps on a compatible iPhone or iPad device
with a developer's provisioning profile. For more information, consult Apple's
developer documentation.

__Note:__ The demo app will not work correctly in the simulator due to native
environment restrictions. Although the demo app will run in the simulator, it will
consistently fail to authenticate with the Approov Cloud Service and you will not be
able to authenticate with the remote Shapes Server.

The demo Xcode project is already configured to use an Approov embedded framework
which is included in `clients/ios/shapes-client`.

* Open shapes-client.xcworkspace (NOT shapes-client.xcodeproj) in Xcode
* In Xcode, configure code signing for the shapes-client target to work with your iOS
developer account so the app can be run on your device.

You can then build and run the app on your device from within Xcode, but it will fail
to authenticate until you have registered it with the Approov Cloud Service.
To authenticate, you must export the app as an IPA file, and then run the Approov
registration tool, which we will get to in a minute.

In Xcode, choose 'Archive' from the 'Product' menu.
The Archives Organizer window will then be displayed, and you can select the archive
and use the 'Export...' button to code sign and export the app as an IPA package.
In the export wizard, choose 'Save for Development Deployment',
click 'Next', choose your Development Team for provisioning,
click 'Next' again to export one app for all compatible devices,
click 'Next' again in the summary page,
then finally export the IPA to a location of your choice.

Once you have a signed IPA you can install and run it on the device.
Either use the iTunes app, Apple Configurator 2 app or the cfgutil command-line tool
to install the IPA.

# Testing it Doesn't Work

To begin with your app should run, but if you try and retrieve a shape it will
fail. This is because simply adding our SDK to the app is not enough. Our servers
need to verify the authenticity of it and to do that they need to have stored
an app signature.

__Android Note:__ In the Android version of the app, there are three buttons to request a shape.
They all have the same effect, but the underlying code is structured to demonstrate Approov
integration with different but common mechanisms for communicating with a remote service.

__iOS Note:__ Be sure to uncheck the debug option if you launch the app from XCode. The Approov demo will always fail attestation if there is an attached debugger whether the app is registered with the Approov service or not.

# Registering an App

For our servers to know about your App, you have to tell us about it. We use a
simple registration program to do this. All you have to do is point our
executable at your App package (APK or IPA depending on your platform) and we
will do the rest. The app signature will be added to the list of recognized signatures in
our attestation servers.

__Note:__ For those behind a firewall, our registration application communicates on port 8087.
If you get an error while submitting data saying you can't establish a connection, this
could be the problem.

It is good practice to register the App whenever it is modified and the process
is simple. In the `registration-tools/` folder, find the executable for your OS.
You will need to provide it with some information to allow it to register your
app. The most important pieces are the App itself and the registration token,
which you received with your demo download link in the email we sent you.
The token authorizes you to upload the new App signature.
You need to place it in a file and feed that to the registration program.

Android on Linux example:

```
$ cd <demo root>/registration-tools/Android/Linux
$ ./registration -a ../../../clients/android/shapes-client/app/build/outputs/apk/debug/app-debug.apk -t <registration_access.tok from email>
```

iOS on OSX example:

```
$ cd <demo root>/registration-tools/iOS
$ registration -a MyApplication.ipa -t registration_access.tok
```

Note: Because this is a test server, we will regularly clear out any
registrations. In a production system you would be able to see exactly what
Apps had been registered in our admin portal and would have complete control
over what App registrations are valid for your system.

# Test It Now Works

Now after a short propagation delay (1-2 minutes) your app will be recognized
as valid and given a token that will let it access the demo server. To make
sure the SDK is not caching an invalid token, you might have to restart the
App.

# Next Steps

If you like, you can now modify the client to explore how to use the SDK.
Remember to register your app again whenever you make a change.

__Note:__ Attaching a debugger will be detected by the SDK and you will not get a valid token.

To take this further and integrate one of your existing apps into the flow, why not progress through
the [full Approov documentation][1]. You can make all the changes required to your client side code
without requiring your own Approov sign-up because the app never knows whether a token is valid or
not anyway.

You can also use the [server-side integration documentation][2] to guide you through the server-side
changes required to receive and validate tokens. We do not publish the secret for the demo, but
once you are set up and receiving tokens from the app, why not
[sign-up to a full Approov account][3] and take advantage of the 1 month free trial to finish off
the verification flow.

If you have any questions or problems then just get in touch via [Zendesk][4].

[1]: https://approov.io/docs/
[2]: https://approov.io/docs/serversideintegration.html
[3]: https://www.approov.io/index.html#pricing
[4]: https://approov.zendesk.com
