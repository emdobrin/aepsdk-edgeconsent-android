# Getting started

## Before starting

The Adobe Experience Platform Consent for Edge Network extension has the following peer dependency, which must be installed prior to installing the consent extension:
- [Mobile Core](https://developer.adobe.com/client-sdks/documentation/mobile-core)

## Configure the Adobe Experience Platform Consent for Edge Network extension in Data Collection UI
1. Log into [Adobe Experience Platform Data Collection](https://experience.adobe.com/data-collection).
2. From **Tags**, locate or search for your Tag mobile property.
3. In your mobile property, select **Extensions** tab.
4. On the **Catalog** tab, locate or search for the **Consent** extension, and select **Install**.
5. Set your desired default consent level.
6. Select **Save**.
7. Follow the [publishing process](https://developer.adobe.com/client-sdks/documentation/getting-started/create-a-mobile-property/#publish-the-configuration) to update SDK configuration.

> **Note**
> In order to ingest and use the data collected by this extension, follow the guide on [ingesting data using the Consents and Preferences data type](https://experienceleague.adobe.com/docs/experience-platform/xdm/data-types/consents.html#ingest).

> **Warning**
> The use of this extension is currently limited to the setting (and enforcement) of client-side, macro consent flags. While SDK APIs allow for granular and global consent preference collection, flags are not consistently enforced with upstream applications and therefore will not accommodate use cases that rely on global/granular consent preferences.

## Add the AEP Consent extension to your app

### Download and import the Consent extension

1. Add the Mobile Core and Edge extensions to your project using the app's Gradle file.

  ```java
implementation 'com.adobe.marketing.mobile:core:2.+'
implementation 'com.adobe.marketing.mobile:edge:2.+'
implementation 'com.adobe.marketing.mobile:edgeidentity:2.+'
implementation 'com.adobe.marketing.mobile:edgeconsent:2.+'
  ```

2. Import the Mobile Core and Edge extensions in your Application class.

  ```java
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.Edge;
import com.adobe.marketing.mobile.edge.identity.Identity;
import com.adobe.marketing.mobile.edge.consent.Consent;
import com.adobe.marketing.mobile.LoggingMode;
import android.app.Application;
import java.util.Arrays;
import android.util.Log;
  ```

3. Register Edge extensions with Mobile Core

  ```java
public class MobileApp extends Application {

  private final String ENVIRONMENT_FILE_ID = "";

   @Override
	public void onCreate() {
		super.onCreate();
		MobileCore.setApplication(this);

		MobileCore.setLogLevel(LoggingMode.DEBUG);
		MobileCore.configureWithAppID(ENVIRONMENT_FILE_ID);

		// register Adobe extensions
		MobileCore.registerExtensions(
				Arrays.asList(Consent.EXTENSION, Edge.EXTENSION, Identity.EXTENSION),
				o -> Log.d("Sample App", "Mobile SDK was initialized")
		);
	}
}
  ```
