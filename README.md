# VelaSMS SDK

[![](https://jitpack.io/v/vela-ng/vela-sms-sdk.svg)](https://jitpack.io/#vela-ng/vela-sms-sdk)


## Getting Started

Follow the instructions below to get started with `VelaOffline` SDK.

### Prerequisites

VelaSMS SDK is in the private domain, to get access you need to request for `accessToken` and once you have that, add it to your `$HOME/.gradle/gradle.properties` for global access or project level `gradle.properties` file.

```
accessToken=jp_XXXXXXXXXXXXXXXXXXXXX

```
Alternatively, you can define it in the `build.gradle` file (app/build.gradle) before using it.

```
def accessToken = "jp_XXXXXXXXXXXXXXXXXXXXXXXX"

...

buildscript {
 ...
```
> We would make use of the `accessToken` below.

### Installing

Follow the steps below to add velaSMS SDK to your project.

1. Add the JitPack repository to your your poject level `build.gradel` file if it is not added already.

    ```Groove
    allprojects {
        repositories {
            ...
            maven { 
            url 'https://jitpack.io'
            credentials { username accessToken }
             }
            ...
        }
    }
    ```
2. Add VelaOffline dependency:

    Open your app level `build.gradel` and add the velaoffline sdk dependency.
    
    ```
    dependencies {
        ....
        implementation 'com.github.vela-ng:vela-sms-sdk:0.0.2'
        ...
    }
    
    ```
3. Sync and build your project.


### Usage 
Follow the instrcution below to configure VelaSMS SDK once you have installed it.

Add the following permission to your `manifest.xml` file.

```xml
 <!-- Used to get the phone IMEI -->
    <uses-permission android:name="android.permission.READ_PHONE_STATE"/>
    
    <uses-permission android:name="android.permission.READ_SMS"/>
    <uses-permission android:name="android.permission.SEND_SMS"/>
    <uses-permission android:name="android.permission.RECEIVE_SMS"/>
```

> Note: You are responsible for requesting all the permissions and making sure that your app has all the permissions granted before using the VelaSMS sdk.
> See the sample app in the repository.


#### Initialize
For this step, you will need to set your `SMS Sort Code`, `Shared Service Code` and `encryption key` In your Application `onCreate()` method, initialize the SDK as shown below:


```
//Init VelaSMS SDK
VelaSMS.init(
             this,
             BuildConfig.SMS_SHORT_CODE,
             BuildConfig.SHARED_SERVICE_CODE,
             BuildConfig.ENCRYPTION_KEY
        );
```

#### Usage
You can use VelaSMS SDK from an `Activity`.

##### Subscribe to Event:
VelaSMSReceiver implement an event bus using `LiveData` that can be observed by any Lifecycle owner as show below:


> The response is astring containing the status code and the pyalod delimtted by a special character `VelaSMSConstants.VELA_DELIMITERS`

Java

```Java
//call vela sms receiver
        VelaSMSReceiver.getSMSEvent().observe(this, new Observer<SSOEvent>() {
            @Override
            public void onChanged(SSOEvent ssoEvent) {
                Timber.d("SSO Event: %s", ssoEvent);
                final String[] parts = ssoEvent.getData().split(VelaSMSConstants.VELA_DELIMITERS);
                final String status = parts[0];

                switch (status) {
                    case VelaSMSConstants.RESPONSE_SUCCESS: {
                        //Use the actual response here.
                        Timber.d("Success Response: %s", parts[1]);
                    }
                    case VelaSMSConstants.RESPONSE_ERROR_CLIENT_APP_ID: {
                        Timber.d("Error: Invalid Client App Id: %s", parts[1]);
                    }
                    case VelaSMSConstants.RESPONSE_ERROR_INVALID_MERCHANT_RESPONSE: {
                        Timber.d("Error: Due to merchant Response: %s", parts[1]);
                    }
                    case VelaSMSConstants.RESPONSE_ERROR: {
                        Timber.d("Unknown Error occurred: %s", parts[1]);
                    }
                }
```
Kotlin

```Kotlin
VelaSMSReceiver.SMSEvent.observe(this, Observer {
            Timber.d("SMS Event: $it")
            val parts = it.data.split(VelaSMSConstants.VELA_DELIMITERS)
            when (parts[0]) {
                VelaSMSConstants.RESPONSE_SUCCESS -> {
                    if (it.isForEncryption.not()) {
                        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
                        val encryptionKey = sharedPref.getString(VelaSMSConstants.PREF_KEY_ENCRYPTION_KEY, "")!!

                        Timber.d("Using Encryption Key: $encryptionKey")

                        val encryption = SecurityUtils.getInstance(encryptionKey)

                        resultTv.text = "Result: ${encryption.decrypt(parts[1].trim())}"
                    } else {
                        resultTv.text = "Result: ${parts[1]}"
                    }

                }
                VelaSMSConstants.RESPONSE_ERROR_CLIENT_APP_ID -> {
                    Timber.d("Error: Invalid Client App Id: ${parts[1]}")
                }
                VelaSMSConstants.RESPONSE_ERROR_INVALID_MERCHANT_RESPONSE -> {
                    Timber.d("Error: Due to merchant Response: ${parts[1]}")
                }
                VelaSMSConstants.RESPONSE_ERROR -> {
                    Timber.d("Unknown Error occurred: ${parts[1]}")
                }
            }
            hideProgress()
        })
```
###### Send Message (Payload)
To send your payload using the sdk, simple call ` VelaSMS.send(Activty, samplePayload);`
Example:

Java

```
@AfterPermissionGranted(RC_VELA_SMS)
    private void sendSMS() {
        final String samplePayload = "1:20:181702773757!48000000:08060000000:NG:t3stt3st:352085099972999";

        //send sms
        VelaSMS.send(this, samplePayload);

    }
```

Kotlin:

```
@SuppressLint("MissingPermission")
    @AfterPermissionGranted(RC_VELA_SMS)
    private fun sendTestSMS() {
        val sampleUSSDLoginRequestOnVelaBank = "0:2*0*08189762414*674342479273842*116051115116116051115116*0"
       
       //send sms
       VelaSMS.send(this@MainActivity, sampleSMSLoginRequestOnVelaBank)
    }
```

##### Observe Progress
If you wish to observer the SMS progress event, then you need to implement `VelaSMSEvent` and overide the neccessary methods. See the sample app for example usage.

```
@Override
    public void hideProgress() {
        progressWrapper.setVisibility(View.GONE);
        progresShowing = false;

    }

    @Override
    public void showError(@NotNull String s) {
        Toast.makeText(this, "An Error occurred: $s", Toast.LENGTH_LONG).show();
    }

    @Override
    public void showProgress() {
        progressWrapper.setVisibility(View.VISIBLE);
        progresShowing = true;
    }

    @Override
    public void updateProgress(@NotNull String s) {
        progressUpdateTv.setText(s);

    }
```

In your Activity's `onCreate()`, add the Activity implementation to the event observer as shown below:

``` 
//add this as an Observer
VelaSMS.addEventObserver(this);

```

In your Activity's `onResume()`, register the broadcast receivers as shown below:

```
@Override
    protected void onResume() {
        super.onResume();
        //register receivers.
        VelaSMS.registerObservers(this);

    }
```

Also in your Activity's `OnPause()`, unregister the broadcaset receivers as shown below:

```
@Override
    protected void onPause() {
        super.onPause();
        //unregister all the receivers
        VelaSMS.unregisterObservers(this);

    }
```


## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/your/project/tags). 
