[1mdiff --git a/android/app/AndroidManifest.xml b/android/app/AndroidManifest.xml[m
[1mindex bdcc793aac..929cc0e42b 100644[m
[1m--- a/android/app/AndroidManifest.xml[m
[1m+++ b/android/app/AndroidManifest.xml[m
[36m@@ -442,7 +442,7 @@[m
              android:process="@string/process"[m
              android:excludeFromRecents="true"[m
              android:theme="@style/dialog"[m
[31m-             android:enabled="false">>[m
[32m+[m[32m             android:enabled="false">[m
         </activity>[m
         <activity android:name="com.android.bluetooth.opp.BluetoothOppBtEnablingActivity"[m
              android:process="@string/process"[m
[1mdiff --git a/android/app/lint-baseline.xml b/android/app/lint-baseline.xml[m
[1mindex 1ba8aa40cd..83e6108f76 100644[m
[1m--- a/android/app/lint-baseline.xml[m
[1m+++ b/android/app/lint-baseline.xml[m
[36m@@ -1,9661 +1,7026 @@[m
 <?xml version="1.0" encoding="UTF-8"?>[m
[31m-<issues format="6" by="lint 8.1.0-dev" type="baseline" client="" dependencies="true" name="" variant="all" version="8.1.0-dev">[m
[32m+[m[32m<issues format="5" by="lint 8.1.0-dev" client="" dependencies="true" name="" type="baseline" variant="all" version="8.1.0-dev">[m
 [m
     <issue[m
         id="MissingClass"[m
[31m-        message="Class referenced in the manifest, `com.android.bluetooth.hfpclient.HfpClientConnectionService`, was not found in the project or the libraries"[m
[31m-        errorLine1="            android:name=&quot;com.android.bluetooth.hfpclient.HfpClientConnectionService&quot;"[m
[31m-        errorLine2="                          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Class referenced in the manifest, `com.android.bluetooth.hfpclient.HfpClientConnectionService`, was not found in the project or the libraries">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="310"[m
[31m-            column="27"/>[m
[32m+[m[32m            line="310"/>[m
[32m+[m[32m    </issue>[m
[32m+[m
[32m+[m[32m    <issue[m
[32m+[m[32m        id="GestureBackNavigation"[m
[32m+[m[32m        message="If intercepting back events, this should be handled through the registration of callbacks on the window level; Please see https://developer.android.com/about/versions/13/features/predictive-back-gesture">[m
[32m+[m[32m        <location[m
[32m+[m[32m            file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/opp/BluetoothOppBtEnablingActivity.java"[m
[32m+[m[32m            line="108"/>[m
[32m+[m[32m    </issue>[m
[32m+[m
[32m+[m[32m    <issue[m
[32m+[m[32m        id="GestureBackNavigation"[m
[32m+[m[32m        message="If intercepting back events, this should be handled through the registration of callbacks on the window level; Please see https://developer.android.com/about/versions/13/features/predictive-back-gesture">[m
[32m+[m[32m        <location[m
[32m+[m[32m            file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/opp/BluetoothOppIncomingFileConfirmActivity.java"[m
[32m+[m[32m            line="181"/>[m
     </issue>[m
 [m
     <issue[m
         id="ScrollViewSize"[m
[31m-        message="This LinearLayout should use `android:layout_height=&quot;wrap_content&quot;`"[m
[31m-        errorLine1="    android:layout_height=&quot;match_parent&quot;"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This LinearLayout should use `android:layout_height=&quot;wrap_content&quot;`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/auth.xml"[m
[31m-            line="26"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="26"/>[m
     </issue>[m
 [m
     <issue[m
         id="ScrollViewSize"[m
[31m-        message="This LinearLayout should use `android:layout_height=&quot;wrap_content&quot;`"[m
[31m-        errorLine1="        android:layout_height=&quot;match_parent&quot;"[m
[31m-        errorLine2="        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This LinearLayout should use `android:layout_height=&quot;wrap_content&quot;`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/bt_enabling_progress.xml"[m
[31m-            line="28"[m
[31m-            column="9"/>[m
[32m+[m[32m            line="28"/>[m
     </issue>[m
 [m
     <issue[m
         id="ScrollViewSize"[m
[31m-        message="This LinearLayout should use `android:layout_height=&quot;wrap_content&quot;`"[m
[31m-        errorLine1="        android:layout_height=&quot;match_parent&quot;"[m
[31m-        errorLine2="        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This LinearLayout should use `android:layout_height=&quot;wrap_content&quot;`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/file_transfer.xml"[m
[31m-            line="26"[m
[31m-            column="9"/>[m
[32m+[m[32m            line="26"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address.toUpperCase());"[m
[31m-        errorLine2="                                                                                              ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/AdapterService.java"[m
[31m-            line="5201"[m
[31m-            column="95"/>[m
[32m+[m[32m            line="5201"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `String.format(Locale, ...)` instead"[m
[31m-        errorLine1="                sb.append(String.format(&quot;    %-17s : %-14s : %3d : %s\n&quot;,"[m
[31m-        errorLine2="                          ^">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `String.format(Locale, ...)` instead">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/avrcp/AvrcpVolumeManager.java"[m
[31m-            line="314"[m
[31m-            column="27"/>[m
[32m+[m[32m            line="314"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="        mType = determineEncoding(encoding.toUpperCase());"[m
[31m-        errorLine2="                                           ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/avrcpcontroller/bip/BipEncoding.java"[m
[31m-            line="94"[m
[31m-            column="44"/>[m
[32m+[m[32m            line="94"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="            proprietaryEncodingId = encoding.substring(4).toUpperCase();"[m
[31m-        errorLine2="                                                          ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/avrcpcontroller/bip/BipEncoding.java"[m
[31m-            line="98"[m
[31m-            column="59"/>[m
[32m+[m[32m            line="98"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="            proprietaryEncodingId = proprietaryId.toUpperCase();"[m
[31m-        errorLine2="                                                  ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/avrcpcontroller/bip/BipEncoding.java"[m
[31m-            line="127"[m
[31m-            column="51"/>[m
[32m+[m[32m            line="127"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toLowerCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="        transformations = transformations.trim().toLowerCase();"[m
[31m-        errorLine2="                                                 ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toLowerCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/avrcpcontroller/bip/BipTransformation.java"[m
[31m-            line="64"[m
[31m-            column="50"/>[m
[32m+[m[32m            line="64"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `String.format(Locale, ...)` instead"[m
[31m-        errorLine1="            String.format(&quot;( %s = %d OR %s = %d OR %s = %d )&quot;, Mms.MESSAGE_TYPE,"[m
[31m-        errorLine2="            ^">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `String.format(Locale, ...)` instead">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapContent.java"[m
[31m-            line="145"[m
[31m-            column="13"/>[m
[32m+[m[32m            line="145"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="                    if (!part.mContentType.toUpperCase().contains(&quot;TEXT&quot;)"[m
[31m-        errorLine2="                                           ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapContent.java"[m
[31m-            line="3765"[m
[31m-            column="44"/>[m
[32m+[m[32m            line="3765"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="                    if (part.mContentType != null &amp;&amp; part.mContentType.toUpperCase()"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapContentObserver.java"[m
[31m-            line="3070"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="3070"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="                    } else if (part.mContentType != null &amp;&amp; part.mContentType.toUpperCase()"[m
[31m-        errorLine2="                                                                              ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapContentObserver.java"[m
[31m-            line="3112"[m
[31m-            column="79"/>[m
[32m+[m[32m            line="3112"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toLowerCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="        return mSubFolders.get(folderName.toLowerCase());"[m
[31m-        errorLine2="                                          ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toLowerCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapFolderElement.java"[m
[31m-            line="263"[m
[31m-            column="43"/>[m
[32m+[m[32m            line="263"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `String.format(Locale, ...)` instead"[m
[31m-        errorLine1="                offsetString = String.format(&quot;%1$02d&quot;, -(offset));"[m
[31m-        errorLine2="                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `String.format(Locale, ...)` instead">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapSmsPdu.java"[m
[31m-            line="398"[m
[31m-            column="32"/>[m
[32m+[m[32m            line="398"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `String.format(Locale, ...)` instead"[m
[31m-        errorLine1="                offsetString = String.format(&quot;%1$02d&quot;, offset);"[m
[31m-        errorLine2="                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `String.format(Locale, ...)` instead">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapSmsPdu.java"[m
[31m-            line="402"[m
[31m-            column="32"/>[m
[32m+[m[32m            line="402"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="            charset = charset.toUpperCase();"[m
[31m-        errorLine2="                              ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapUtils.java"[m
[31m-            line="624"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="624"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="            } else if (!line.toUpperCase().contains(subString.toUpperCase())) {"[m
[31m-        errorLine2="                             ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapbMessage.java"[m
[31m-            line="414"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="414"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="            } else if (!line.toUpperCase().contains(subString.toUpperCase())) {"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapbMessage.java"[m
[31m-            line="414"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="414"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="            if (!line.toUpperCase().contains(subString.toUpperCase())) {"[m
[31m-        errorLine2="                      ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapbMessage.java"[m
[31m-            line="427"[m
[31m-            column="23"/>[m
[32m+[m[32m            line="427"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="            if (!line.toUpperCase().contains(subString.toUpperCase())) {"[m
[31m-        errorLine2="                                                       ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapbMessage.java"[m
[31m-            line="427"[m
[31m-            column="56"/>[m
[32m+[m[32m            line="427"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="            if (!line.toUpperCase().contains(subString2.toUpperCase())) {"[m
[31m-        errorLine2="                      ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapbMessage.java"[m
[31m-            line="431"[m
[31m-            column="23"/>[m
[32m+[m[32m            line="431"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="            if (!line.toUpperCase().contains(subString2.toUpperCase())) {"[m
[31m-        errorLine2="                                                        ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapbMessage.java"[m
[31m-            line="431"[m
[31m-            column="57"/>[m
[32m+[m[32m            line="431"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="                charset = charset.toUpperCase();"[m
[31m-        errorLine2="                                  ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapbMessageMime.java"[m
[31m-            line="57"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="57"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="                if (mContentType != null &amp;&amp; (mContentType.toUpperCase().contains(&quot;TEXT&quot;)"[m
[31m-        errorLine2="                                                          ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapbMessageMime.java"[m
[31m-            line="104"[m
[31m-            column="59"/>[m
[32m+[m[32m            line="104"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="                        || mContentType.toUpperCase().contains(&quot;SMIL&quot;))) {"[m
[31m-        errorLine2="                                        ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapbMessageMime.java"[m
[31m-            line="105"[m
[31m-            column="41"/>[m
[32m+[m[32m            line="105"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="            if (mContentType != null &amp;&amp; mContentType.toUpperCase().contains(&quot;TEXT&quot;)) {"[m
[31m-        errorLine2="                                                     ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapbMessageMime.java"[m
[31m-            line="128"[m
[31m-            column="54"/>[m
[32m+[m[32m            line="128"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="            } else if (mContentType != null &amp;&amp; mContentType.toUpperCase().contains(&quot;/SMIL&quot;)) {"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapbMessageMime.java"[m
[31m-            line="134"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="134"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="                if (part.mContentType.toUpperCase().contains(&quot;TEXT&quot;)) {"[m
[31m-        errorLine2="                                      ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapbMessageMime.java"[m
[31m-            line="186"[m
[31m-            column="39"/>[m
[32m+[m[32m            line="186"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="                if (part.mContentType != null &amp;&amp; part.mContentType.toUpperCase().contains(&quot;TEXT&quot;)) {"[m
[31m-        errorLine2="                                                                   ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapbMessageMime.java"[m
[31m-            line="351"[m
[31m-            column="68"/>[m
[32m+[m[32m            line="351"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="            String headerType = headerParts[0].toUpperCase();"[m
[31m-        errorLine2="                                               ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapbMessageMime.java"[m
[31m-            line="583"[m
[31m-            column="48"/>[m
[32m+[m[32m            line="583"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="                String headerType = headerParts[0].toUpperCase();"[m
[31m-        errorLine2="                                                   ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapbMessageMime.java"[m
[31m-            line="680"[m
[31m-            column="52"/>[m
[32m+[m[32m            line="680"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toLowerCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="                        String value = contentTypeParts[j].toLowerCase();"[m
[31m-        errorLine2="                                                           ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toLowerCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapbMessageMime.java"[m
[31m-            line="687"[m
[31m-            column="60"/>[m
[32m+[m[32m            line="687"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="        if (encoding != null &amp;&amp; encoding.toUpperCase().contains(&quot;BASE64&quot;)) {"[m
[31m-        errorLine2="                                         ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapbMessageMime.java"[m
[31m-            line="730"[m
[31m-            column="42"/>[m
[32m+[m[32m            line="730"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="        } else if (encoding != null &amp;&amp; encoding.toUpperCase().contains(&quot;QUOTED-PRINTABLE&quot;)) {"[m
[31m-        errorLine2="                                                ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapbMessageMime.java"[m
[31m-            line="732"[m
[31m-            column="49"/>[m
[32m+[m[32m            line="732"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toLowerCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="            extension = name.substring(dotIndex + 1).toLowerCase();"[m
[31m-        errorLine2="                                                     ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toLowerCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/opp/BluetoothOppObexServerSession.java"[m
[31m-            line="256"[m
[31m-            column="54"/>[m
[32m+[m[32m            line="256"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toLowerCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="            mimeType = mimeType.toLowerCase();"[m
[31m-        errorLine2="                                ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toLowerCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/opp/BluetoothOppObexServerSession.java"[m
[31m-            line="272"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="272"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toLowerCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="                compareValue = appParamValue.searchValue.trim().toLowerCase();"[m
[31m-        errorLine2="                                                                ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toLowerCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/pbap/BluetoothPbapObexServer.java"[m
[31m-            line="945"[m
[31m-            column="65"/>[m
[32m+[m[32m            line="945"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toLowerCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="                            || ((currentValue.toLowerCase())"[m
[31m-        errorLine2="                                              ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toLowerCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/pbap/BluetoothPbapObexServer.java"[m
[31m-            line="957"[m
[31m-            column="47"/>[m
[32m+[m[32m            line="957"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toLowerCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="                                               .startsWith(compareValue.toLowerCase()))) {"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toLowerCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/pbap/BluetoothPbapObexServer.java"[m
[31m-            line="958"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="958"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toLowerCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="        return bdAddr.toLowerCase().startsWith(carkitAddr.toLowerCase());"[m
[31m-        errorLine2="                      ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toLowerCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/DeviceWorkArounds.java"[m
[31m-            line="34"[m
[31m-            column="23"/>[m
[32m+[m[32m            line="34"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toLowerCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="        return bdAddr.toLowerCase().startsWith(carkitAddr.toLowerCase());"[m
[31m-        errorLine2="                                                          ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toLowerCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/DeviceWorkArounds.java"[m
[31m-            line="34"[m
[31m-            column="59"/>[m
[32m+[m[32m            line="34"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `String.format(Locale, ...)` instead"[m
[31m-        errorLine1="            Folder playerFolder = new Folder(String.format(&quot;%02d&quot;, id), false, displayName);"[m
[31m-        errorLine2="                                             ~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `String.format(Locale, ...)` instead">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/audio_util/MediaPlayerList.java"[m
[31m-            line="454"[m
[31m-            column="46"/>[m
[32m+[m[32m            line="454"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `String.format(Locale, ...)` instead"[m
[31m-        errorLine1="                String playerPrefix = String.format(&quot;%02d&quot;, playerIndex);"[m
[31m-        errorLine2="                                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `String.format(Locale, ...)` instead">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/audio_util/MediaPlayerList.java"[m
[31m-            line="516"[m
[31m-            column="39"/>[m
[32m+[m[32m            line="516"/>[m
     </issue>[m
 [m
     <issue[m
         id="DefaultLocale"[m
[31m-        message="Implicitly using the default locale is a common source of bugs: Use `toLowerCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."[m
[31m-        errorLine1="                &quot;[^a-zA-Z0-9 ]&quot;, &quot;&quot;).toLowerCase().split(&quot; &quot;);"[m
[31m-        errorLine2="                                     ~~~~~~~~~~~">[m
[32m+[m[32m        message="Implicitly using the default locale is a common source of bugs: Use `toLowerCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/MetricsLogger.java"[m
[31m-            line="281"[m
[31m-            column="38"/>[m
[32m+[m[32m            line="281"/>[m
     </issue>[m
 [m
     <issue[m
         id="NewApi"[m
[31m-        message="Call requires API level 34 (current min is 33): `android.media.AudioManager#setLeAudioSuspended`"[m
[31m-        errorLine1="                mSystemInterface.getAudioManager().setLeAudioSuspended(true);"[m
[31m-        errorLine2="                                                   ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Call requires API level 34 (current min is 33): `android.media.AudioManager#setLeAudioSuspended`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1853"[m
[31m-            column="52"/>[m
[32m+[m[32m            line="1855"/>[m
     </issue>[m
 [m
     <issue[m
         id="NewApi"[m
[31m-        message="Call requires API level 34 (current min is 33): `android.media.AudioManager#setLeAudioSuspended`"[m
[31m-        errorLine1="                mSystemInterface.getAudioManager().setLeAudioSuspended(false);"[m
[31m-        errorLine2="                                                   ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Call requires API level 34 (current min is 33): `android.media.AudioManager#setLeAudioSuspended`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1864"[m
[31m-            column="52"/>[m
[32m+[m[32m            line="1866"/>[m
     </issue>[m
 [m
     <issue[m
         id="NewApi"[m
[31m-        message="Call requires API level 34 (current min is 33): `android.media.AudioManager#setLeAudioSuspended`"[m
[31m-        errorLine1="                    mSystemInterface.getAudioManager().setLeAudioSuspended(false);"[m
[31m-        errorLine2="                                                       ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Call requires API level 34 (current min is 33): `android.media.AudioManager#setLeAudioSuspended`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="2100"[m
[31m-            column="56"/>[m
[32m+[m[32m            line="2102"/>[m
     </issue>[m
 [m
     <issue[m
         id="NewApi"[m
[31m-        message="Call requires API level 34 (current min is 33): `android.media.AudioManager#setLeAudioSuspended`"[m
[31m-        errorLine1="                    mSystemInterface.getAudioManager().setLeAudioSuspended(true);"[m
[31m-        errorLine2="                                                       ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Call requires API level 34 (current min is 33): `android.media.AudioManager#setLeAudioSuspended`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetStateMachine.java"[m
[31m-            line="1124"[m
[31m-            column="56"/>[m
[32m+[m[32m            line="1150"/>[m
     </issue>[m
 [m
     <issue[m
         id="NewApi"[m
[31m-        message="Call requires API level 34 (current min is 33): `android.media.AudioManager#setLeAudioSuspended`"[m
[31m-        errorLine1="                        mSystemInterface.getAudioManager().setLeAudioSuspended(false);"[m
[31m-        errorLine2="                                                           ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Call requires API level 34 (current min is 33): `android.media.AudioManager#setLeAudioSuspended`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetStateMachine.java"[m
[31m-            line="1127"[m
[31m-            column="60"/>[m
[32m+[m[32m            line="1153"/>[m
     </issue>[m
 [m
     <issue[m
         id="NewApi"[m
[31m-        message="Call requires API level 34 (current min is 33): `android.media.BluetoothProfileConnectionInfo#createLeAudioOutputInfo`"[m
[31m-        errorLine1="                    previousActiveOutDevice, BluetoothProfileConnectionInfo.createLeAudioOutputInfo("[m
[31m-        errorLine2="                                                                            ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Call requires API level 34 (current min is 33): `android.media.BluetoothProfileConnectionInfo#createLeAudioOutputInfo`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/le_audio/LeAudioService.java"[m
[31m-            line="1328"[m
[31m-            column="77"/>[m
[32m+[m[32m            line="1328"/>[m
     </issue>[m
 [m
     <issue[m
         id="NewApi"[m
[31m-        message="Call requires API level 34 (current min is 33): `android.media.BluetoothProfileConnectionInfo#createLeAudioOutputInfo`"[m
[31m-        errorLine1="                    mActiveAudioOutDevice, BluetoothProfileConnectionInfo.createLeAudioOutputInfo("[m
[31m-        errorLine2="                                                                          ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Call requires API level 34 (current min is 33): `android.media.BluetoothProfileConnectionInfo#createLeAudioOutputInfo`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/le_audio/LeAudioService.java"[m
[31m-            line="3082"[m
[31m-            column="75"/>[m
[32m+[m[32m            line="3082"/>[m
     </issue>[m
 [m
     <issue[m
         id="SimpleDateFormat"[m
[31m-        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates."[m
[31m-        errorLine1="    static final DateFormat DATE_FORMAT = new SimpleDateFormat(&quot;MM-dd HH:mm:ss&quot;);"[m
[31m-        errorLine2="                                          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/gatt/AppScanStats.java"[m
[31m-            line="51"[m
[31m-            column="43"/>[m
[32m+[m[32m            line="51"/>[m
     </issue>[m
 [m
     <issue[m
         id="SimpleDateFormat"[m
[31m-        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates."[m
[31m-        errorLine1="        SimpleDateFormat format = new SimpleDateFormat(&quot;yyyyMMdd&apos;T&apos;HHmmss&quot;);"[m
[31m-        errorLine2="                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapAppParams.java"[m
[31m-            line="857"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="857"/>[m
     </issue>[m
 [m
     <issue[m
         id="SimpleDateFormat"[m
[31m-        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates."[m
[31m-        errorLine1="        SimpleDateFormat format = new SimpleDateFormat(&quot;yyyyMMdd&apos;T&apos;HHmmss&quot;);"[m
[31m-        errorLine2="                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapAppParams.java"[m
[31m-            line="867"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="867"/>[m
     </issue>[m
 [m
     <issue[m
         id="SimpleDateFormat"[m
[31m-        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates."[m
[31m-        errorLine1="        SimpleDateFormat format = new SimpleDateFormat(&quot;yyyyMMdd&apos;T&apos;HHmmss&quot;);"[m
[31m-        errorLine2="                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapAppParams.java"[m
[31m-            line="877"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="877"/>[m
     </issue>[m
 [m
     <issue[m
         id="SimpleDateFormat"[m
[31m-        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates."[m
[31m-        errorLine1="        SimpleDateFormat format = new SimpleDateFormat(&quot;yyyyMMdd&apos;T&apos;HHmmss&quot;);"[m
[31m-        errorLine2="                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapAppParams.java"[m
[31m-            line="887"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="887"/>[m
     </issue>[m
 [m
     <issue[m
         id="SimpleDateFormat"[m
[31m-        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates."[m
[31m-        errorLine1="        SimpleDateFormat format = new SimpleDateFormat(&quot;yyyyMMdd&apos;T&apos;HHmmss&quot;);"[m
[31m-        errorLine2="                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapAppParams.java"[m
[31m-            line="901"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="901"/>[m
     </issue>[m
 [m
     <issue[m
         id="SimpleDateFormat"[m
[31m-        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates."[m
[31m-        errorLine1="        SimpleDateFormat format = new SimpleDateFormat(&quot;yyyyMMdd&apos;T&apos;HHmmss&quot;);"[m
[31m-        errorLine2="                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapAppParams.java"[m
[31m-            line="911"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="911"/>[m
     </issue>[m
 [m
     <issue[m
         id="SimpleDateFormat"[m
[31m-        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates."[m
[31m-        errorLine1="        SimpleDateFormat format = new SimpleDateFormat(&quot;yyyyMMdd&apos;T&apos;HHmmss&quot;);"[m
[31m-        errorLine2="                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapAppParams.java"[m
[31m-            line="917"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="917"/>[m
     </issue>[m
 [m
     <issue[m
         id="SimpleDateFormat"[m
[31m-        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates."[m
[31m-        errorLine1="        SimpleDateFormat format = new SimpleDateFormat(&quot;yyyyMMdd&apos;T&apos;HHmmss&quot;);"[m
[31m-        errorLine2="                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapAppParams.java"[m
[31m-            line="927"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="927"/>[m
     </issue>[m
 [m
     <issue[m
         id="SimpleDateFormat"[m
[31m-        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates."[m
[31m-        errorLine1="        SimpleDateFormat format = new SimpleDateFormat(&quot;yyyyMMdd&apos;T&apos;HHmmssZ&quot;);"[m
[31m-        errorLine2="                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapAppParams.java"[m
[31m-            line="1125"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="1125"/>[m
     </issue>[m
 [m
     <issue[m
         id="SimpleDateFormat"[m
[31m-        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates."[m
[31m-        errorLine1="        SimpleDateFormat format = new SimpleDateFormat(&quot;yyyyMMdd&apos;T&apos;HHmmssZ&quot;);"[m
[31m-        errorLine2="                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapAppParams.java"[m
[31m-            line="1135"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="1135"/>[m
     </issue>[m
 [m
     <issue[m
         id="SimpleDateFormat"[m
[31m-        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates."[m
[31m-        errorLine1="        SimpleDateFormat format = new SimpleDateFormat(&quot;yyyyMMdd&apos;T&apos;HHmmssZ&quot;);"[m
[31m-        errorLine2="                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapAppParams.java"[m
[31m-            line="1371"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="1371"/>[m
     </issue>[m
 [m
     <issue[m
         id="SimpleDateFormat"[m
[31m-        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates."[m
[31m-        errorLine1="        SimpleDateFormat format = new SimpleDateFormat(&quot;yyyyMMdd&apos;T&apos;HHmmssZ&quot;);"[m
[31m-        errorLine2="                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapAppParams.java"[m
[31m-            line="1381"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="1381"/>[m
     </issue>[m
 [m
     <issue[m
         id="SimpleDateFormat"[m
[31m-        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates."[m
[31m-        errorLine1="        SimpleDateFormat format = new SimpleDateFormat(&quot;yyyyMMdd&apos;T&apos;HHmmss&quot;);"[m
[31m-        errorLine2="                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapConvoContactElement.java"[m
[31m-            line="159"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="159"/>[m
     </issue>[m
 [m
     <issue[m
         id="SimpleDateFormat"[m
[31m-        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates."[m
[31m-        errorLine1="        SimpleDateFormat format = new SimpleDateFormat(&quot;yyyyMMdd&apos;T&apos;HHmmss&quot;);"[m
[31m-        errorLine2="                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapConvoContactElement.java"[m
[31m-            line="169"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="169"/>[m
     </issue>[m
 [m
     <issue[m
         id="SimpleDateFormat"[m
[31m-        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates."[m
[31m-        errorLine1="        SimpleDateFormat format = new SimpleDateFormat(&quot;yyyyMMdd&apos;T&apos;HHmmss&quot;);"[m
[31m-        errorLine2="                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapConvoListingElement.java"[m
[31m-            line="149"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="149"/>[m
     </issue>[m
 [m
     <issue[m
         id="SimpleDateFormat"[m
[31m-        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates."[m
[31m-        errorLine1="        SimpleDateFormat format = new SimpleDateFormat(&quot;yyyyMMdd&apos;T&apos;HHmmss&quot;);"[m
[31m-        errorLine2="                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapConvoListingElement.java"[m
[31m-            line="163"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="163"/>[m
     </issue>[m
 [m
     <issue[m
         id="SimpleDateFormat"[m
[31m-        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates."[m
[31m-        errorLine1="        SimpleDateFormat format = new SimpleDateFormat(&quot;yyyyMMdd&apos;T&apos;HHmmss&quot;);"[m
[31m-        errorLine2="                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapMessageListingElement.java"[m
[31m-            line="83"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="83"/>[m
     </issue>[m
 [m
     <issue[m
         id="SimpleDateFormat"[m
[31m-        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates."[m
[31m-        errorLine1="            SimpleDateFormat format = new SimpleDateFormat(&quot;yyMMddHHmmss&quot;);"[m
[31m-        errorLine2="                                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapSmsPdu.java"[m
[31m-            line="380"[m
[31m-            column="39"/>[m
[32m+[m[32m            line="380"/>[m
     </issue>[m
 [m
     <issue[m
         id="SimpleDateFormat"[m
[31m-        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates."[m
[31m-        errorLine1="        SimpleDateFormat format = (mPeerSupportUtcTimeStamp) ? new"[m
[31m-        errorLine2="                                                               ^">[m
[32m+[m[32m        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapUtils.java"[m
[31m-            line="702"[m
[31m-            column="64"/>[m
[32m+[m[32m            line="702"/>[m
     </issue>[m
 [m
     <issue[m
         id="SimpleDateFormat"[m
[31m-        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates."[m
[31m-        errorLine1="            SimpleDateFormat(&quot;yyyyMMdd&apos;T&apos;HHmmssZ&quot;) : new SimpleDateFormat(&quot;yyyyMMdd&apos;T&apos;HHmmss&quot;);"[m
[31m-        errorLine2="                                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapUtils.java"[m
[31m-            line="703"[m
[31m-            column="54"/>[m
[32m+[m[32m            line="703"/>[m
     </issue>[m
 [m
     <issue[m
         id="SimpleDateFormat"[m
[31m-        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates."[m
[31m-        errorLine1="        DateFormat dateFormat = new SimpleDateFormat(&quot;_hhmmss&quot;);"[m
[31m-        errorLine2="                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/opp/BluetoothOppReceiveFileInfo.java"[m
[31m-            line="170"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="170"/>[m
     </issue>[m
 [m
     <issue[m
         id="SimpleDateFormat"[m
[31m-        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates."[m
[31m-        errorLine1="        SimpleDateFormat df = new SimpleDateFormat(RFC_2455_FORMAT);"[m
[31m-        errorLine2="                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/pbap/BluetoothPbapCallLogComposer.java"[m
[31m-            line="211"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="211"/>[m
     </issue>[m
 [m
     <issue[m
         id="SimpleDateFormat"[m
[31m-        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates."[m
[31m-        errorLine1="                SimpleDateFormat parser = new SimpleDateFormat(TIMESTAMP_FORMAT);"[m
[31m-        errorLine2="                                          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, or `getTimeInstance()`, or use `new SimpleDateFormat(String template, Locale locale)` with for example `Locale.US` for ASCII dates.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/pbapclient/CallLogPullRequest.java"[m
[31m-            line="111"[m
[31m-            column="43"/>[m
[32m+[m[32m            line="111"/>[m
     </issue>[m
 [m
     <issue[m
         id="InflateParams"[m
[31m-        message="Avoid passing `null` as the view root (needed to resolve layout parameters on the inflated layout&apos;s root element)"[m
[31m-        errorLine1="            convertView = mInflater.inflate(R.layout.bluetooth_map_settings_account_item, null);"[m
[31m-        errorLine2="                                                                                          ~~~~">[m
[32m+[m[32m        message="Avoid passing `null` as the view root (needed to resolve layout parameters on the inflated layout&apos;s root element)">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapSettingsAdapter.java"[m
[31m-            line="110"[m
[31m-            column="91"/>[m
[32m+[m[32m            line="110"/>[m
     </issue>[m
 [m
     <issue[m
         id="InflateParams"[m
[31m-        message="Avoid passing `null` as the view root (needed to resolve layout parameters on the inflated layout&apos;s root element)"[m
[31m-        errorLine1="            convertView = mInflater.inflate(R.layout.bluetooth_map_settings_account_group, null);"[m
[31m-        errorLine2="                                                                                           ~~~~">[m
[32m+[m[32m        message="Avoid passing `null` as the view root (needed to resolve layout parameters on the inflated layout&apos;s root element)">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapSettingsAdapter.java"[m
[31m-            line="231"[m
[31m-            column="92"/>[m
[32m+[m[32m            line="231"/>[m
     </issue>[m
 [m
     <issue[m
         id="InflateParams"[m
[31m-        message="Avoid passing `null` as the view root (needed to resolve layout parameters on the inflated layout&apos;s root element)"[m
[31m-        errorLine1="        View view = getLayoutInflater().inflate(R.layout.confirm_dialog, null);"[m
[31m-        errorLine2="                                                                         ~~~~">[m
[32m+[m[32m        message="Avoid passing `null` as the view root (needed to resolve layout parameters on the inflated layout&apos;s root element)">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/opp/BluetoothOppBtEnableActivity.java"[m
[31m-            line="74"[m
[31m-            column="74"/>[m
[32m+[m[32m            line="74"/>[m
     </issue>[m
 [m
     <issue[m
         id="InflateParams"[m
[31m-        message="Avoid passing `null` as the view root (needed to resolve layout parameters on the inflated layout&apos;s root element)"[m
[31m-        errorLine1="        View view = getLayoutInflater().inflate(R.layout.bt_enabling_progress, null);"[m
[31m-        errorLine2="                                                                               ~~~~">[m
[32m+[m[32m        message="Avoid passing `null` as the view root (needed to resolve layout parameters on the inflated layout&apos;s root element)">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/opp/BluetoothOppBtEnablingActivity.java"[m
[31m-            line="99"[m
[31m-            column="80"/>[m
[32m+[m[32m            line="99"/>[m
     </issue>[m
 [m
     <issue[m
         id="InflateParams"[m
[31m-        message="Avoid passing `null` as the view root (needed to resolve layout parameters on the inflated layout&apos;s root element)"[m
[31m-        errorLine1="        View view = getLayoutInflater().inflate(R.layout.confirm_dialog, null);"[m
[31m-        errorLine2="                                                                         ~~~~">[m
[32m+[m[32m        message="Avoid passing `null` as the view root (needed to resolve layout parameters on the inflated layout&apos;s root element)">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/opp/BluetoothOppBtErrorActivity.java"[m
[31m-            line="68"[m
[31m-            column="74"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="InflateParams"[m
[31m-        message="Avoid passing `null` as the view root (needed to resolve layout parameters on the inflated layout&apos;s root element)"[m
[31m-        errorLine1="        View view = getLayoutInflater().inflate(R.layout.incoming_dialog, null);"[m
[31m-        errorLine2="                                                                          ~~~~">[m
[32m+[m[32m        message="Avoid passing `null` as the view root (needed to resolve layout parameters on the inflated layout&apos;s root element)">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/opp/BluetoothOppIncomingFileConfirmActivity.java"[m
[31m-            line="140"[m
[31m-            column="75"/>[m
[32m+[m[32m            line="140"/>[m
     </issue>[m
 [m
     <issue[m
         id="InflateParams"[m
[31m-        message="Avoid passing `null` as the view root (needed to resolve layout parameters on the inflated layout&apos;s root element)"[m
[31m-        errorLine1="        mView = getLayoutInflater().inflate(R.layout.file_transfer, null);"[m
[31m-        errorLine2="                                                                    ~~~~">[m
[32m+[m[32m        message="Avoid passing `null` as the view root (needed to resolve layout parameters on the inflated layout&apos;s root element)">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/opp/BluetoothOppTransferActivity.java"[m
[31m-            line="243"[m
[31m-            column="69"/>[m
[32m+[m[32m            line="243"/>[m
     </issue>[m
 [m
     <issue[m
         id="InflateParams"[m
[31m-        message="Avoid passing `null` as the view root (needed to resolve layout parameters on the inflated layout&apos;s root element)"[m
[31m-        errorLine1="                mView = getLayoutInflater().inflate(R.layout.auth, null);"[m
[31m-        errorLine2="                                                                   ~~~~">[m
[32m+[m[32m        message="Avoid passing `null` as the view root (needed to resolve layout parameters on the inflated layout&apos;s root element)">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/pbap/BluetoothPbapActivity.java"[m
[31m-            line="158"[m
[31m-            column="68"/>[m
[32m+[m[32m            line="158"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mDatabaseManager = Objects.requireNonNull(mAdapterService.getDatabase(),"[m
[31m-        errorLine2="                                                                  ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/a2dp/A2dpService.java"[m
[31m-            line="146"[m
[31m-            column="67"/>[m
[32m+[m[32m            line="146"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (!Utils.arrayContains(mAdapterService.getRemoteUuids(device),"[m
[31m-        errorLine2="                                                 ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/a2dp/A2dpService.java"[m
[31m-            line="289"[m
[31m-            column="50"/>[m
[32m+[m[32m            line="289"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                if (!Utils.arrayContains(mAdapterService.getRemoteUuids(device),"[m
[31m-        errorLine2="                                                         ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/a2dp/A2dpService.java"[m
[31m-            line="447"[m
[31m-            column="58"/>[m
[32m+[m[32m            line="447"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (!mDatabaseManager.setProfileConnectionPolicy(device, BluetoothProfile.A2DP,"[m
[31m-        errorLine2="                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/a2dp/A2dpService.java"[m
[31m-            line="674"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="674"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                .getProfileConnectionPolicy(device, BluetoothProfile.A2DP);"[m
[31m-        errorLine2="                 ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/a2dp/A2dpService.java"[m
[31m-            line="700"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="700"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        return mDatabaseManager.getA2dpSupportsOptionalCodecs(device);"[m
[31m-        errorLine2="                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/a2dp/A2dpService.java"[m
[31m-            line="863"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="863"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mDatabaseManager.setA2dpSupportsOptionalCodecs(device, value);"[m
[31m-        errorLine2="                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/a2dp/A2dpService.java"[m
[31m-            line="869"[m
[31m-            column="26"/>[m
[32m+[m[32m            line="869"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        return mDatabaseManager.getA2dpOptionalCodecsEnabled(device);"[m
[31m-        errorLine2="                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/a2dp/A2dpService.java"[m
[31m-            line="882"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="882"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mDatabaseManager.setA2dpOptionalCodecsEnabled(device, value);"[m
[31m-        errorLine2="                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/a2dp/A2dpService.java"[m
[31m-            line="902"[m
[31m-            column="26"/>[m
[32m+[m[32m            line="902"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            sm.sendMessage(A2dpStateMachine.STACK_EVENT, stackEvent);"[m
[31m-        errorLine2="                                            ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/a2dp/A2dpService.java"[m
[31m-            line="975"[m
[31m-            column="45"/>[m
[32m+[m[32m            line="975"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            if (mAdapterService.getBondState(device) == BluetoothDevice.BOND_NONE) {"[m
[31m-        errorLine2="                                ~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/a2dp/A2dpService.java"[m
[31m-            line="1254"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="1254"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        DatabaseManager dbManager = mAdapterService.getDatabase();"[m
[31m-        errorLine2="                                                    ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/a2dp/A2dpService.java"[m
[31m-            line="1267"[m
[31m-            column="53"/>[m
[32m+[m[32m            line="1267"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mDatabaseManager = Objects.requireNonNull(AdapterService.getAdapterService().getDatabase(),"[m
[31m-        errorLine2="                                                                                     ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/a2dpsink/A2dpSinkService.java"[m
[31m-            line="75"[m
[31m-            column="86"/>[m
[32m+[m[32m            line="75"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (!mDatabaseManager.setProfileConnectionPolicy(device, BluetoothProfile.A2DP_SINK,"[m
[31m-        errorLine2="                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/a2dpsink/A2dpSinkService.java"[m
[31m-            line="520"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="520"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                .getProfileConnectionPolicy(device, BluetoothProfile.A2DP_SINK);"[m
[31m-        errorLine2="                 ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/a2dpsink/A2dpSinkService.java"[m
[31m-            line="543"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="543"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mService.removeStateMachine(A2dpSinkStateMachine.this);"[m
[31m-        errorLine2="                             ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/a2dpsink/A2dpSinkStateMachine.java"[m
[31m-            line="163"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="163"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                DatabaseManager dbManager = mAdapterService.getDatabase();"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/ActiveDeviceManager.java"[m
[31m-            line="284"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="284"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                if (dbManager.getProfileConnectionPolicy(device, BluetoothProfile.HEADSET)"[m
[31m-        errorLine2="                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/ActiveDeviceManager.java"[m
[31m-            line="285"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="285"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                DatabaseManager dbManager = mAdapterService.getDatabase();"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/ActiveDeviceManager.java"[m
[31m-            line="351"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="351"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                if (dbManager.getProfileConnectionPolicy(device, BluetoothProfile.A2DP)"[m
[31m-        errorLine2="                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/ActiveDeviceManager.java"[m
[31m-            line="352"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="352"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        DatabaseManager dbManager = mAdapterService.getDatabase();"[m
[31m-        errorLine2="                                                    ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/ActiveDeviceManager.java"[m
[31m-            line="951"[m
[31m-            column="53"/>[m
[32m+[m[32m            line="951"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        DatabaseManager dbManager = mAdapterService.getDatabase();"[m
[31m-        errorLine2="                                                    ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/ActiveDeviceManager.java"[m
[31m-            line="1153"[m
[31m-            column="53"/>[m
[32m+[m[32m            line="1153"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        byte[] deviceType = dbManager.getCustomMeta(device,"[m
[31m-        errorLine2="                                      ~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/ActiveDeviceManager.java"[m
[31m-            line="1154"[m
[31m-            column="39"/>[m
[32m+[m[32m            line="1154"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mMapService = BluetoothMapService.getBluetoothMapService();"[m
[31m-        errorLine2="                                          ~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/AdapterService.java"[m
[31m-            line="1631"[m
[31m-            column="43"/>[m
[32m+[m[32m            line="1631"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mPbapService = BluetoothPbapService.getBluetoothPbapService();"[m
[31m-        errorLine2="                                            ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/AdapterService.java"[m
[31m-            line="1636"[m
[31m-            column="45"/>[m
[32m+[m[32m            line="1636"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mSapService = SapService.getSapService();"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/AdapterService.java"[m
[31m-            line="1640"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="1640"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        return mDatabaseManager.setCustomMeta(device, key, value);"[m
[31m-        errorLine2="                                ~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/AdapterService.java"[m
[31m-            line="1880"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="1880"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        return mDatabaseManager.getCustomMeta(device, key);"[m
[31m-        errorLine2="                                ~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/AdapterService.java"[m
[31m-            line="1891"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="1891"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mRemoteDevices.updateBatteryLevel(device, batteryLevel);"[m
[31m-        errorLine2="                       ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/AdapterService.java"[m
[31m-            line="7004"[m
[31m-            column="24"/>[m
[32m+[m[32m            line="7004"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mNativeInterface.atResponseCode(device, atCommandResult, atCommandErrorCode);"[m
[31m-        errorLine2="                                     ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/AtPhonebook.java"[m
[31m-            line="192"[m
[31m-            column="38"/>[m
[32m+[m[32m            line="192"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            mNativeInterface.atResponseString(device, atCommandResponse);"[m
[31m-        errorLine2="                             ~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/AtPhonebook.java"[m
[31m-            line="211"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="211"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mNativeInterface.atResponseCode(device, atCommandResult, atCommandErrorCode);"[m
[31m-        errorLine2="                         ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/AtPhonebook.java"[m
[31m-            line="213"[m
[31m-            column="26"/>[m
[32m+[m[32m            line="213"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            mNativeInterface.atResponseString(device, atCommandResponse);"[m
[31m-        errorLine2="                             ~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/AtPhonebook.java"[m
[31m-            line="280"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="280"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mNativeInterface.atResponseCode(device, atCommandResult, atCommandErrorCode);"[m
[31m-        errorLine2="                         ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/AtPhonebook.java"[m
[31m-            line="282"[m
[31m-            column="26"/>[m
[32m+[m[32m            line="282"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        mNativeInterface.atResponseCode(remoteDevice, atCommandResult,"[m
[31m-        errorLine2="                                         ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/AtPhonebook.java"[m
[31m-            line="305"[m
[31m-            column="42"/>[m
[32m+[m[32m            line="305"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mNativeInterface.atResponseString(remoteDevice, atCommandResponse);"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/AtPhonebook.java"[m
[31m-            line="320"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="320"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mNativeInterface.atResponseCode(remoteDevice, atCommandResult, atCommandErrorCode);"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/AtPhonebook.java"[m
[31m-            line="321"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="321"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mNativeInterface.atResponseCode(remoteDevice, atCommandResult,"[m
[31m-        errorLine2="                                     ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/AtPhonebook.java"[m
[31m-            line="332"[m
[31m-            column="38"/>[m
[32m+[m[32m            line="332"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mNativeInterface.atResponseCode(remoteDevice, atCommandResult,"[m
[31m-        errorLine2="                                     ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/AtPhonebook.java"[m
[31m-            line="340"[m
[31m-            column="38"/>[m
[32m+[m[32m            line="340"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mNativeInterface.atResponseCode(remoteDevice, atCommandResult,"[m
[31m-        errorLine2="                                     ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/AtPhonebook.java"[m
[31m-            line="360"[m
[31m-            column="38"/>[m
[32m+[m[32m            line="360"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mNativeInterface.atResponseCode(remoteDevice, atCommandResult,"[m
[31m-        errorLine2="                                     ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/AtPhonebook.java"[m
[31m-            line="373"[m
[31m-            column="38"/>[m
[32m+[m[32m            line="373"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mNativeInterface.atResponseCode(remoteDevice,"[m
[31m-        errorLine2="                                     ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/AtPhonebook.java"[m
[31m-            line="379"[m
[31m-            column="38"/>[m
[32m+[m[32m            line="379"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mNativeInterface.atResponseCode(remoteDevice, atCommandResult, atCommandErrorCode);"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/AtPhonebook.java"[m
[31m-            line="391"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="391"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            mNativeInterface.atResponseString(device, atCommandResponse);"[m
[31m-        errorLine2="                             ~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/AtPhonebook.java"[m
[31m-            line="627"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="627"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mDatabaseManager = Objects.requireNonNull(mAdapterService.getDatabase(),"[m
[31m-        errorLine2="                                                                  ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="251"[m
[31m-            column="67"/>[m
[32m+[m[32m            line="264"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            case BassClientStateMachine.ADD_BCAST_SOURCE:"[m
[31m-        errorLine2="                                        ~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="409"[m
[31m-            column="41"/>[m
[32m+[m[32m            line="422"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                            (m.first.equals(BassClientStateMachine.ADD_BCAST_SOURCE))"[m
[31m-        errorLine2="                                                                   ~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="418"[m
[31m-            column="68"/>[m
[32m+[m[32m            line="431"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                            (m.first.equals(BassClientStateMachine.ADD_BCAST_SOURCE))"[m
[31m-        errorLine2="                                                                   ~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="428"[m
[31m-            column="68"/>[m
[32m+[m[32m            line="441"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            case BassClientStateMachine.REMOVE_BCAST_SOURCE:"[m
[31m-        errorLine2="                                        ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="433"[m
[31m-            column="41"/>[m
[32m+[m[32m            line="446"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        m.first.equals(BassClientStateMachine.REMOVE_BCAST_SOURCE)"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="437"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="450"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    stateMachine.getCurrentBroadcastMetadata(sourceId);"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="470"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="483"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                            getOrCreateStateMachine(device).getAllSources();"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="476"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="489"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        for (BluetoothLeBroadcastReceiveState recvState: stateMachine.getAllSources()) {"[m
[31m-        errorLine2="                                                                      ~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="554"[m
[31m-            column="71"/>[m
[32m+[m[32m            line="567"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            sm.doQuit();"[m
[31m-        errorLine2="               ~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="614"[m
[31m-            column="16"/>[m
[32m+[m[32m            line="627"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            sm.cleanup();"[m
[31m-        errorLine2="               ~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="615"[m
[31m-            column="16"/>[m
[32m+[m[32m            line="628"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            int bondState = mAdapterService.getBondState(device);"[m
[31m-        errorLine2="                                            ~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="639"[m
[31m-            column="45"/>[m
[32m+[m[32m            line="652"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            stateMachine.sendMessage(BassClientStateMachine.CONNECT);"[m
[31m-        errorLine2="                                                            ~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="690"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="703"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            stateMachine.sendMessage(BassClientStateMachine.DISCONNECT);"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="711"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="724"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            return sm.getConnectionState();"[m
[31m-        errorLine2="                      ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="760"[m
[31m-            column="23"/>[m
[32m+[m[32m            line="773"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    connectionState = sm.getConnectionState();"[m
[31m-        errorLine2="                                         ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="788"[m
[31m-            column="42"/>[m
[32m+[m[32m            line="801"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                if (sm.isConnected()) {"[m
[31m-        errorLine2="                       ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="809"[m
[31m-            column="24"/>[m
[32m+[m[32m            line="822"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    devices.add(sm.getDevice());"[m
[31m-        errorLine2="                                   ~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="810"[m
[31m-            column="36"/>[m
[32m+[m[32m            line="823"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mDatabaseManager.setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="835"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="848"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                .getProfileConnectionPolicy(device, BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT);"[m
[31m-        errorLine2="                 ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="859"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="872"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                                if (sm.isConnected()) {"[m
[31m-        errorLine2="                                       ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="941"[m
[31m-            column="40"/>[m
[32m+[m[32m            line="954"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                                    selectSource(sm.getDevice(), result, false);"[m
[31m-        errorLine2="                                                    ~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="942"[m
[31m-            column="53"/>[m
[32m+[m[32m            line="955"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    BassClientStateMachine.SELECT_BCAST_SOURCE);"[m
[31m-        errorLine2="                                           ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="1022"[m
[31m-            column="44"/>[m
[32m+[m[32m            line="1035"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            if (stateMachine.hasPendingSourceOperation()) {"[m
[31m-        errorLine2="                             ~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="1072"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="1085"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                enqueueSourceGroupOp(device, BassClientStateMachine.ADD_BCAST_SOURCE,"[m
[31m-        errorLine2="                                                                    ~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="1098"[m
[31m-            column="69"/>[m
[32m+[m[32m            line="1111"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            Message message = stateMachine.obtainMessage(BassClientStateMachine.ADD_BCAST_SOURCE);"[m
[31m-        errorLine2="                                                                                ~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="1102"[m
[31m-            column="81"/>[m
[32m+[m[32m            line="1115"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                message = stateMachine.obtainMessage(BassClientStateMachine.SET_BCAST_CODE);"[m
[31m-        errorLine2="                                                                            ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="1106"[m
[31m-            column="77"/>[m
[32m+[m[32m            line="1119"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                message.arg1 = BassClientStateMachine.ARGTYPE_METADATA;"[m
[31m-        errorLine2="                                                      ~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="1108"[m
[31m-            column="55"/>[m
[32m+[m[32m            line="1121"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            if (stateMachine.hasPendingSourceOperation()) {"[m
[31m-        errorLine2="                             ~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="1169"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="1182"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    stateMachine.obtainMessage(BassClientStateMachine.UPDATE_BCAST_SOURCE);"[m
[31m-        errorLine2="                                                                      ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="1174"[m
[31m-            column="71"/>[m
[32m+[m[32m            line="1187"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                message = stateMachine.obtainMessage(BassClientStateMachine.SET_BCAST_CODE);"[m
[31m-        errorLine2="                                                                            ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="1180"[m
[31m-            column="77"/>[m
[32m+[m[32m            line="1193"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                message.arg1 = BassClientStateMachine.ARGTYPE_METADATA;"[m
[31m-        errorLine2="                                                      ~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="1182"[m
[31m-            column="55"/>[m
[32m+[m[32m            line="1195"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    stateMachine.getBroadcastReceiveStateForSourceId(sourceId);"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="1223"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="1236"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    stateMachine.getCurrentBroadcastMetadata(sourceId);"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="1225"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="1238"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        BassClientStateMachine.UPDATE_BCAST_SOURCE);"[m
[31m-        errorLine2="                                               ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="1230"[m
[31m-            column="48"/>[m
[32m+[m[32m            line="1243"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    stateMachine.obtainMessage(BassClientStateMachine.REMOVE_BCAST_SOURCE);"[m
[31m-        errorLine2="                                                                      ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="1241"[m
[31m-            column="71"/>[m
[32m+[m[32m            line="1254"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            enqueueSourceGroupOp(device, BassClientStateMachine.REMOVE_BCAST_SOURCE,"[m
[31m-        errorLine2="                                                                ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="1249"[m
[31m-            column="65"/>[m
[32m+[m[32m            line="1262"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            for (BluetoothLeBroadcastReceiveState rs: stateMachine.getAllSources()) {"[m
[31m-        errorLine2="                                                                   ~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="1270"[m
[31m-            column="68"/>[m
[32m+[m[32m            line="1283"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        return stateMachine.getMaximumSourceCapacity();"[m
[31m-        errorLine2="                            ~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="1293"[m
[31m-            column="29"/>[m
[32m+[m[32m            line="1306"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                            BassClientStateMachine.ADD_BCAST_SOURCE, param.mObj2);"[m
[31m-        errorLine2="                                                   ~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="1368"[m
[31m-            column="52"/>[m
[32m+[m[32m            line="1381"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                            BassClientStateMachine.REMOVE_BCAST_SOURCE, Integer.valueOf(msg.arg2));"[m
[31m-        errorLine2="                                                   ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassClientService.java"[m
[31m-            line="1374"[m
[31m-            column="52"/>[m
[32m+[m[32m            line="1387"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        return BassClientStateMachine.make(device, svc, looper);"[m
[31m-        errorLine2="                                      ~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassObjectsFactory.java"[m
[31m-            line="75"[m
[31m-            column="39"/>[m
[32m+[m[32m            line="75"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        BassClientStateMachine.destroy(stateMachine);"[m
[31m-        errorLine2="                               ~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bass_client/BassObjectsFactory.java"[m
[31m-            line="84"[m
[31m-            column="32"/>[m
[32m+[m[32m            line="84"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mDatabaseManager = Objects.requireNonNull(mAdapterService.getDatabase(),"[m
[31m-        errorLine2="                                                                  ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bas/BatteryService.java"[m
[31m-            line="98"[m
[31m-            column="67"/>[m
[32m+[m[32m            line="98"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        ParcelUuid[] featureUuids = mAdapterService.getRemoteUuids(device);"[m
[31m-        errorLine2="                                                    ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bas/BatteryService.java"[m
[31m-            line="210"[m
[31m-            column="53"/>[m
[32m+[m[32m            line="210"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        mAdapterService.getRemoteUuids(device), BluetoothUuid.BATTERY)) {"[m
[31m-        errorLine2="                                        ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bas/BatteryService.java"[m
[31m-            line="237"[m
[31m-            column="41"/>[m
[32m+[m[32m            line="237"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mDatabaseManager.setProfileConnectionPolicy(device, BluetoothProfile.BATTERY,"[m
[31m-        errorLine2="                         ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bas/BatteryService.java"[m
[31m-            line="421"[m
[31m-            column="26"/>[m
[32m+[m[32m            line="421"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        return mDatabaseManager.getProfileConnectionPolicy(device, BluetoothProfile.BATTERY);"[m
[31m-        errorLine2="                                ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/bas/BatteryService.java"[m
[31m-            line="438"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="438"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            call.answer(VideoProfile.STATE_AUDIO_ONLY);"[m
[31m-        errorLine2="                 ~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="386"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="386"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            BluetoothCall conferenceCall = getBluetoothCallById(call.getParentId());"[m
[31m-        errorLine2="                                                                     ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="401"[m
[31m-            column="70"/>[m
[32m+[m[32m            line="401"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    &amp;&amp; conferenceCall.getState() == Call.STATE_ACTIVE) {"[m
[31m-        errorLine2="                                      ~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="403"[m
[31m-            column="39"/>[m
[32m+[m[32m            line="403"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            if (call.getState() == Call.STATE_RINGING) {"[m
[31m-        errorLine2="                     ~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="407"[m
[31m-            column="22"/>[m
[32m+[m[32m            line="407"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                call.reject(false, &quot;&quot;);"[m
[31m-        errorLine2="                     ~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="408"[m
[31m-            column="22"/>[m
[32m+[m[32m            line="408"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                call.disconnect();"[m
[31m-        errorLine2="                     ~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="410"[m
[31m-            column="22"/>[m
[32m+[m[32m            line="410"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            call.playDtmfTone((char) dtmf);"[m
[31m-        errorLine2="                 ~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="427"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="427"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            call.stopDtmfTone();"[m
[31m-        errorLine2="                 ~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="428"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="428"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (call.isExternalCall()) {"[m
[31m-        errorLine2="                 ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="571"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="575"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (!mBluetoothCallHashMap.containsKey(call.getId())) {"[m
[31m-        errorLine2="                                                    ~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="574"[m
[31m-            column="53"/>[m
[32m+[m[32m            line="578"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            CallStateCallback callback = new CallStateCallback(call.getState());"[m
[31m-        errorLine2="                                                                    ~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="576"[m
[31m-            column="69"/>[m
[32m+[m[32m            line="580"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            mCallbacks.put(call.getId(), callback);"[m
[31m-        errorLine2="                                ~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="577"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="581"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            call.registerCallback(callback);"[m
[31m-        errorLine2="                 ~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="578"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="582"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            mBluetoothCallHashMap.put(call.getId(), call);"[m
[31m-        errorLine2="                                           ~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="580"[m
[31m-            column="44"/>[m
[32m+[m[32m            line="584"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            if (!call.isConference()) {"[m
[31m-        errorLine2="                      ~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="581"[m
[31m-            column="23"/>[m
[32m+[m[32m            line="585"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        call.sendCallEvent("[m
[31m-        errorLine2="             ~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="617"[m
[31m-            column="14"/>[m
[32m+[m[32m            line="621"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        onCallAdded(new BluetoothCall(call));"[m
[31m-        errorLine2="                    ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="624"[m
[31m-            column="21"/>[m
[32m+[m[32m            line="628"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (callback != null &amp;&amp; (forceRemoveCallback || !call.isExternalCall())) {"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="639"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="643"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            call.unregisterCallback(callback);"[m
[31m-        errorLine2="                 ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="640"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="644"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (mBluetoothCallHashMap.containsKey(call.getId())) {"[m
[31m-        errorLine2="                                                   ~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="643"[m
[31m-            column="52"/>[m
[32m+[m[32m            line="647"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            mBluetoothCallHashMap.remove(call.getId());"[m
[31m-        errorLine2="                                              ~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="644"[m
[31m-            column="47"/>[m
[32m+[m[32m            line="648"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            DisconnectCause cause = call.getDisconnectCause();"[m
[31m-        errorLine2="                                         ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="646"[m
[31m-            column="42"/>[m
[32m+[m[32m            line="650"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mBluetoothCallQueue.add(call.getId());"[m
[31m-        errorLine2="                                             ~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="649"[m
[31m-            column="46"/>[m
[32m+[m[32m            line="653"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mBluetoothConferenceCallInference.put(call.getId(), call);"[m
[31m-        errorLine2="                                                           ~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="650"[m
[31m-            column="60"/>[m
[32m+[m[32m            line="654"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            if (call.isConference()) {"[m
[31m-        errorLine2="                     ~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="665"[m
[31m-            column="22"/>[m
[32m+[m[32m            line="669"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            mBluetoothLeCallControl.onCallRemoved(call.getTbsCallId(), getTbsTerminationReason(call));"[m
[31m-        errorLine2="                                                       ~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="677"[m
[31m-            column="56"/>[m
[32m+[m[32m            line="681"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        return call.isConference()"[m
[31m-        errorLine2="                    ~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="755"[m
[31m-            column="21"/>[m
[32m+[m[32m            line="759"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            &amp;&amp; (call.can(Connection.CAPABILITY_CONFERENCE_HAS_NO_CHILDREN)"[m
[31m-        errorLine2="                     ~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="756"[m
[31m-            column="22"/>[m
[32m+[m[32m            line="760"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    || call.getChildrenIds().isEmpty());"[m
[31m-        errorLine2="                            ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="757"[m
[31m-            column="29"/>[m
[32m+[m[32m            line="761"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            if (isInferenceEnabled &amp;&amp; call.isConference()"[m
[31m-        errorLine2="                                           ~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="766"[m
[31m-            column="44"/>[m
[32m+[m[32m            line="770"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                if (call.getChildrenIds().size() >= 2) {"[m
[31m-        errorLine2="                         ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="778"[m
[31m-            column="26"/>[m
[32m+[m[32m            line="782"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    int direction = inferredCall.isIncoming() ? 1 : 0;"[m
[31m-        errorLine2="                                                 ~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="795"[m
[31m-            column="50"/>[m
[32m+[m[32m            line="799"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    if (inferredCall.getGatewayInfo() != null) {"[m
[31m-        errorLine2="                                     ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="799"[m
[31m-            column="38"/>[m
[32m+[m[32m            line="803"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        addressUri = inferredCall.getGatewayInfo().getOriginalAddress();"[m
[31m-        errorLine2="                                                  ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="800"[m
[31m-            column="51"/>[m
[32m+[m[32m            line="804"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        addressUri = inferredCall.getHandle();"[m
[31m-        errorLine2="                                                  ~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="802"[m
[31m-            column="51"/>[m
[32m+[m[32m            line="806"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                + &quot;, call.getChildrenIds() size &quot; + call.getChildrenIds().size());"[m
[31m-        errorLine2="                                                         ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="852"[m
[31m-            column="58"/>[m
[32m+[m[32m            line="856"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            if (!call.isConference() || isConferenceWithNoChildren) {"[m
[31m-        errorLine2="                      ~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="853"[m
[31m-            column="23"/>[m
[32m+[m[32m            line="857"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        BluetoothCall conferenceCall = getBluetoothCallById(call.getParentId());"[m
[31m-        errorLine2="                                                                 ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="880"[m
[31m-            column="66"/>[m
[32m+[m[32m            line="884"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            if (conferenceCall.hasProperty(Call.Details.PROPERTY_GENERIC_CONFERENCE)) {"[m
[31m-        errorLine2="                               ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="884"[m
[31m-            column="32"/>[m
[32m+[m[32m            line="888"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                                conferenceCall.getGenericConferenceActiveChildCallId());"[m
[31m-        errorLine2="                                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="896"[m
[31m-            column="48"/>[m
[32m+[m[32m            line="900"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                            conferenceCall.can(Connection.CAPABILITY_MERGE_CONFERENCE)"[m
[31m-        errorLine2="                                           ~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="901"[m
[31m-            column="44"/>[m
[32m+[m[32m            line="905"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                                    || (conferenceCall.can(Connection.CAPABILITY_SWAP_CONFERENCE)"[m
[31m-        errorLine2="                                                       ~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="902"[m
[31m-            column="56"/>[m
[32m+[m[32m            line="906"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                                            &amp;&amp; !conferenceCall.wasConferencePreviouslyMerged());"[m
[31m-        errorLine2="                                                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="903"[m
[31m-            column="64"/>[m
[32m+[m[32m            line="907"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            if (conferenceCall.getState() == Call.STATE_HOLDING"[m
[31m-        errorLine2="                               ~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="917"[m
[31m-            column="32"/>[m
[32m+[m[32m            line="921"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    &amp;&amp; conferenceCall.can(Connection.CAPABILITY_MANAGE_CONFERENCE)) {"[m
[31m-        errorLine2="                                      ~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="918"[m
[31m-            column="39"/>[m
[32m+[m[32m            line="922"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        int direction = call.isIncoming() ? 1 : 0;"[m
[31m-        errorLine2="                             ~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="933"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="937"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (call.getGatewayInfo() != null) {"[m
[31m-        errorLine2="                 ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="935"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="939"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            addressUri = call.getGatewayInfo().getOriginalAddress();"[m
[31m-        errorLine2="                              ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="936"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="940"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            addressUri = call.getHandle();"[m
[31m-        errorLine2="                              ~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="938"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="942"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (mCallInfo.isNullCall(call) || call.getHandle() == null) {"[m
[31m-        errorLine2="                                               ~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="971"[m
[31m-            column="48"/>[m
[32m+[m[32m            line="975"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        Uri handle = call.getHandle();"[m
[31m-        errorLine2="                          ~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="974"[m
[31m-            column="27"/>[m
[32m+[m[32m            line="978"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (call.hasProperty(Call.Details.PROPERTY_SELF_MANAGED)) {"[m
[31m-        errorLine2="                 ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="976"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="980"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            key = handle.toString() + &quot; self managed &quot; + call.getId();"[m
[31m-        errorLine2="                                                              ~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="977"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="981"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (call.isConference()) {"[m
[31m-        errorLine2="                 ~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="994"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="998"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                ringingCall.reject(false, null);"[m
[31m-        errorLine2="                            ~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1029"[m
[31m-            column="29"/>[m
[32m+[m[32m            line="1033"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                heldCall.disconnect();"[m
[31m-        errorLine2="                         ~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1032"[m
[31m-            column="26"/>[m
[32m+[m[32m            line="1036"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                BluetoothCall conferenceCall = getBluetoothCallById(activeCall.getParentId());"[m
[31m-        errorLine2="                                                                               ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1042"[m
[31m-            column="80"/>[m
[32m+[m[32m            line="1046"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        &amp;&amp; conferenceCall.getState() == Call.STATE_ACTIVE) {"[m
[31m-        errorLine2="                                          ~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1044"[m
[31m-            column="43"/>[m
[32m+[m[32m            line="1048"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    conferenceCall.disconnect();"[m
[31m-        errorLine2="                                   ~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1046"[m
[31m-            column="36"/>[m
[32m+[m[32m            line="1050"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    activeCall.disconnect();"[m
[31m-        errorLine2="                               ~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1048"[m
[31m-            column="32"/>[m
[32m+[m[32m            line="1052"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                ringingCall.answer(ringingCall.getVideoState());"[m
[31m-        errorLine2="                            ~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1052"[m
[31m-            column="29"/>[m
[32m+[m[32m            line="1056"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                ringingCall.answer(ringingCall.getVideoState());"[m
[31m-        errorLine2="                                               ~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1052"[m
[31m-            column="48"/>[m
[32m+[m[32m            line="1056"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                heldCall.unhold();"[m
[31m-        errorLine2="                         ~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1054"[m
[31m-            column="26"/>[m
[32m+[m[32m            line="1058"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    &amp;&amp; activeCall.can(Connection.CAPABILITY_SWAP_CONFERENCE)) {"[m
[31m-        errorLine2="                                  ~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1059"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="1063"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                activeCall.swapConference();"[m
[31m-        errorLine2="                           ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1060"[m
[31m-            column="28"/>[m
[32m+[m[32m            line="1064"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                ringingCall.answer(VideoProfile.STATE_AUDIO_ONLY);"[m
[31m-        errorLine2="                            ~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1065"[m
[31m-            column="29"/>[m
[32m+[m[32m            line="1069"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                heldCall.unhold();"[m
[31m-        errorLine2="                         ~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1070"[m
[31m-            column="26"/>[m
[32m+[m[32m            line="1074"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    &amp;&amp; activeCall.can(Connection.CAPABILITY_HOLD)) {"[m
[31m-        errorLine2="                                  ~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1073"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="1077"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                activeCall.hold();"[m
[31m-        errorLine2="                           ~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1074"[m
[31m-            column="28"/>[m
[32m+[m[32m            line="1078"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                if (activeCall.can(Connection.CAPABILITY_MERGE_CONFERENCE)) {"[m
[31m-        errorLine2="                               ~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1079"[m
[31m-            column="32"/>[m
[32m+[m[32m            line="1083"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    activeCall.mergeConference();"[m
[31m-        errorLine2="                               ~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1080"[m
[31m-            column="32"/>[m
[32m+[m[32m            line="1084"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                            activeCall.getConferenceableCalls());"[m
[31m-        errorLine2="                                       ~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1084"[m
[31m-            column="40"/>[m
[32m+[m[32m            line="1088"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        activeCall.conference(conferenceable.get(0));"[m
[31m-        errorLine2="                                   ~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1086"[m
[31m-            column="36"/>[m
[32m+[m[32m            line="1090"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (!mCallInfo.isNullCall(ringingCall) &amp;&amp; ringingCall.getHandle() != null"[m
[31m-        errorLine2="                                                              ~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1112"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="1116"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                &amp;&amp; !ringingCall.isSilentRingingRequested()) {"[m
[31m-        errorLine2="                                ~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1113"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="1117"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            ringingAddress = ringingCall.getHandle().getSchemeSpecificPart();"[m
[31m-        errorLine2="                                         ~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1114"[m
[31m-            column="42"/>[m
[32m+[m[32m            line="1118"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            ringingName = ringingCall.getCallerDisplayName();"[m
[31m-        errorLine2="                                      ~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1118"[m
[31m-            column="39"/>[m
[32m+[m[32m            line="1122"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                ringingName = ringingCall.getContactDisplayName();"[m
[31m-        errorLine2="                                          ~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1120"[m
[31m-            column="43"/>[m
[32m+[m[32m            line="1124"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mCallInfo.isNullCall(activeCall) ? 0 : activeCall.getChildrenIds().size();"[m
[31m-        errorLine2="                                                                  ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1130"[m
[31m-            column="67"/>[m
[32m+[m[32m            line="1134"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (!mCallInfo.isNullCall(activeCall) &amp;&amp; activeCall.isConference()"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1141"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="1145"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                &amp;&amp; !activeCall.can(Connection.CAPABILITY_CONFERENCE_HAS_NO_CHILDREN)) {"[m
[31m-        errorLine2="                               ~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1142"[m
[31m-            column="32"/>[m
[32m+[m[32m            line="1146"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            if (activeCall.can(Connection.CAPABILITY_SWAP_CONFERENCE)) {"[m
[31m-        errorLine2="                           ~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1143"[m
[31m-            column="28"/>[m
[32m+[m[32m            line="1147"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                numHeldCalls = activeCall.wasConferencePreviouslyMerged() ? 0 : 1;"[m
[31m-        errorLine2="                                          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1146"[m
[31m-            column="43"/>[m
[32m+[m[32m            line="1150"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            } else if (activeCall.can(Connection.CAPABILITY_MERGE_CONFERENCE)) {"[m
[31m-        errorLine2="                                  ~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1147"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="1151"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            for (Integer id : activeCall.getChildrenIds()) {"[m
[31m-        errorLine2="                                         ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1151"[m
[31m-            column="42"/>[m
[32m+[m[32m            line="1155"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                if (mOldHeldCall != null &amp;&amp; Objects.equals(mOldHeldCall.getId(), id)) {"[m
[31m-        errorLine2="                                                                        ~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1155"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="1159"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (!mCallInfo.isNullCall(ringingCall) &amp;&amp; !ringingCall.isSilentRingingRequested()) {"[m
[31m-        errorLine2="                                                               ~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1236"[m
[31m-            column="64"/>[m
[32m+[m[32m            line="1240"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        switch (call.getState()) {"[m
[31m-        errorLine2="                     ~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1248"[m
[31m-            column="22"/>[m
[32m+[m[32m            line="1252"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                if (call.isSilentRingingRequested()) {"[m
[31m-        errorLine2="                         ~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1275"[m
[31m-            column="26"/>[m
[32m+[m[32m            line="1279"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        switch (call.getState()) {"[m
[31m-        errorLine2="                     ~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1480"[m
[31m-            column="22"/>[m
[32m+[m[32m            line="1488"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            if (call.isSilentRingingRequested()) {"[m
[31m-        errorLine2="                     ~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1497"[m
[31m-            column="22"/>[m
[32m+[m[32m            line="1505"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        BluetoothCall conferenceCall = getBluetoothCallById(call.getParentId());"[m
[31m-        errorLine2="                                                                 ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1544"[m
[31m-            column="66"/>[m
[32m+[m[32m            line="1552"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                getBluetoothCallById(conferenceCall.getGenericConferenceActiveChildCallId());"[m
[31m-        errorLine2="                                                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1559"[m
[31m-            column="53"/>[m
[32m+[m[32m            line="1567"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        conferenceCall.can(Connection.CAPABILITY_MERGE_CONFERENCE)"[m
[31m-        errorLine2="                                       ~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1564"[m
[31m-            column="40"/>[m
[32m+[m[32m            line="1572"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                                    || (conferenceCall.can(Connection.CAPABILITY_SWAP_CONFERENCE)"[m
[31m-        errorLine2="                                                       ~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1565"[m
[31m-            column="56"/>[m
[32m+[m[32m            line="1573"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                                    &amp;&amp; !conferenceCall.wasConferencePreviouslyMerged());"[m
[31m-        errorLine2="                                                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1566"[m
[31m-            column="56"/>[m
[32m+[m[32m            line="1574"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            if (conferenceCall.getState() == Call.STATE_HOLDING"[m
[31m-        errorLine2="                               ~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1579"[m
[31m-            column="32"/>[m
[32m+[m[32m            line="1587"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    &amp;&amp; conferenceCall.can(Connection.CAPABILITY_MANAGE_CONFERENCE)) {"[m
[31m-        errorLine2="                                      ~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1580"[m
[31m-            column="39"/>[m
[32m+[m[32m            line="1588"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (call.getGatewayInfo() != null) {"[m
[31m-        errorLine2="                 ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1596"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="1604"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            addressUri = call.getGatewayInfo().getOriginalAddress();"[m
[31m-        errorLine2="                              ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1597"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="1605"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            addressUri = call.getHandle();"[m
[31m-        errorLine2="                              ~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1599"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="1607"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        int callFlags = call.isIncoming() ? 0 : BluetoothLeCall.FLAG_OUTGOING_CALL;"[m
[31m-        errorLine2="                             ~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1603"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="1611"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        String friendlyName = call.getCallerDisplayName();"[m
[31m-        errorLine2="                                   ~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1605"[m
[31m-            column="36"/>[m
[32m+[m[32m            line="1613"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            friendlyName = call.getContactDisplayName();"[m
[31m-        errorLine2="                                ~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1607"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="1615"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        return new BluetoothLeCall(call.getTbsCallId(), uri, friendlyName, state, callFlags);"[m
[31m-        errorLine2="                                        ~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/telephony/BluetoothInCallService.java"[m
[31m-            line="1610"[m
[31m-            column="41"/>[m
[32m+[m[32m            line="1618"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (!mDatabaseManager.setProfileConnectionPolicy(device, BluetoothProfile.MAP,"[m
[31m-        errorLine2="                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapService.java"[m
[31m-            line="639"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="639"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                .getProfileConnectionPolicy(device, BluetoothProfile.MAP);"[m
[31m-        errorLine2="                 ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapService.java"[m
[31m-            line="666"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="666"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mDatabaseManager = Objects.requireNonNull(AdapterService.getAdapterService().getDatabase(),"[m
[31m-        errorLine2="                                                                                     ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapService.java"[m
[31m-            line="680"[m
[31m-            column="86"/>[m
[32m+[m[32m            line="680"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within package private scope"[m
[31m-        errorLine1="        return new AppAdvertiseStats(appUid, id, name, map, service);"[m
[31m-        errorLine2="               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within package private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/BluetoothMethodProxy.java"[m
[31m-            line="259"[m
[31m-            column="16"/>[m
[32m+[m[32m            line="259"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (!mDatabaseManager.setProfileConnectionPolicy(device, BluetoothProfile.PBAP,"[m
[31m-        errorLine2="                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/pbap/BluetoothPbapService.java"[m
[31m-            line="595"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="595"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                .getProfileConnectionPolicy(device, BluetoothProfile.PBAP);"[m
[31m-        errorLine2="                 ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/pbap/BluetoothPbapService.java"[m
[31m-            line="622"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="622"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mDatabaseManager = Objects.requireNonNull(AdapterService.getAdapterService().getDatabase(),"[m
[31m-        errorLine2="                                                                                     ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/pbap/BluetoothPbapService.java"[m
[31m-            line="661"[m
[31m-            column="86"/>[m
[32m+[m[32m            line="661"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            mRemoteDevices.addDeviceProperties(address);"[m
[31m-        errorLine2="                           ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/BondStateMachine.java"[m
[31m-            line="523"[m
[31m-            column="28"/>[m
[32m+[m[32m            line="523"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            mRemoteDevices.addDeviceProperties(address);"[m
[31m-        errorLine2="                           ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/BondStateMachine.java"[m
[31m-            line="559"[m
[31m-            column="28"/>[m
[32m+[m[32m            line="559"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            mRemoteDevices.addDeviceProperties(address);"[m
[31m-        errorLine2="                           ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/BondStateMachine.java"[m
[31m-            line="586"[m
[31m-            column="28"/>[m
[32m+[m[32m            line="586"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mDatabaseManager = Objects.requireNonNull(mAdapterService.getDatabase(),"[m
[31m-        errorLine2="                                                                  ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/csip/CsipSetCoordinatorService.java"[m
[31m-            line="141"[m
[31m-            column="67"/>[m
[32m+[m[32m            line="141"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        ParcelUuid[] featureUuids = mAdapterService.getRemoteUuids(device);"[m
[31m-        errorLine2="                                                    ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/csip/CsipSetCoordinatorService.java"[m
[31m-            line="288"[m
[31m-            column="53"/>[m
[32m+[m[32m            line="288"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mDatabaseManager.setProfileConnectionPolicy("[m
[31m-        errorLine2="                         ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/csip/CsipSetCoordinatorService.java"[m
[31m-            line="487"[m
[31m-            column="26"/>[m
[32m+[m[32m            line="487"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        return mDatabaseManager.getProfileConnectionPolicy("[m
[31m-        errorLine2="                                ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/csip/CsipSetCoordinatorService.java"[m
[31m-            line="505"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="505"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                sm.sendMessage(CsipSetCoordinatorStateMachine.STACK_EVENT, stackEvent);"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/csip/CsipSetCoordinatorService.java"[m
[31m-            line="916"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="916"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mDatabaseManager = Objects.requireNonNull(mAdapterService.getDatabase(),"[m
[31m-        errorLine2="                                                                  ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hap/HapClientService.java"[m
[31m-            line="161"[m
[31m-            column="67"/>[m
[32m+[m[32m            line="161"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mDatabaseManager.setProfileConnectionPolicy(device, BluetoothProfile.HAP_CLIENT,"[m
[31m-        errorLine2="                         ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hap/HapClientService.java"[m
[31m-            line="380"[m
[31m-            column="26"/>[m
[32m+[m[32m            line="380"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        return mDatabaseManager.getProfileConnectionPolicy(device, BluetoothProfile.HAP_CLIENT);"[m
[31m-        errorLine2="                                ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hap/HapClientService.java"[m
[31m-            line="403"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="403"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        ParcelUuid[] featureUuids = mAdapterService.getRemoteUuids(device);"[m
[31m-        errorLine2="                                                    ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hap/HapClientService.java"[m
[31m-            line="476"[m
[31m-            column="53"/>[m
[32m+[m[32m            line="476"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            sm.sendMessage(HapClientStateMachine.STACK_EVENT, stackEvent);"[m
[31m-        errorLine2="                                                 ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hap/HapClientService.java"[m
[31m-            line="1207"[m
[31m-            column="50"/>[m
[32m+[m[32m            line="1207"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    AdapterService.getAdapterService().getDatabase(),"[m
[31m-        errorLine2="                                                       ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientService.java"[m
[31m-            line="109"[m
[31m-            column="56"/>[m
[32m+[m[32m            line="109"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            mNativeInterface.initialize();"[m
[31m-        errorLine2="                             ~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientService.java"[m
[31m-            line="114"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="114"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            mNativeInterface.cleanup();"[m
[31m-        errorLine2="                             ~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientService.java"[m
[31m-            line="188"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="188"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (!mDatabaseManager.setProfileConnectionPolicy(device, BluetoothProfile.HEADSET_CLIENT,"[m
[31m-        errorLine2="                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientService.java"[m
[31m-            line="840"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="840"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                .getProfileConnectionPolicy(device, BluetoothProfile.HEADSET_CLIENT);"[m
[31m-        errorLine2="                 ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientService.java"[m
[31m-            line="866"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="866"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mNativeInterface.queryCurrentCalls(mCurrentDevice);"[m
[31m-        errorLine2="                         ~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java"[m
[31m-            line="422"[m
[31m-            column="26"/>[m
[32m+[m[32m            line="422"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (mNativeInterface.handleCallAction(mCurrentDevice, action, 0)) {"[m
[31m-        errorLine2="                             ~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java"[m
[31m-            line="658"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="658"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (mNativeInterface.handleCallAction(mCurrentDevice, action, 0)) {"[m
[31m-        errorLine2="                             ~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java"[m
[31m-            line="697"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="697"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (mNativeInterface.handleCallAction(mCurrentDevice, action, 0)) {"[m
[31m-        errorLine2="                             ~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java"[m
[31m-            line="722"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="722"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            if (mNativeInterface.handleCallAction(mCurrentDevice, action, 0)) {"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java"[m
[31m-            line="743"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="743"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            mNativeInterface.disconnect(mCurrentDevice);"[m
[31m-        errorLine2="                             ~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java"[m
[31m-            line="978"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="978"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    if (!mNativeInterface.connect(device)) {"[m
[31m-        errorLine2="                                          ~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java"[m
[31m-            line="1067"[m
[31m-            column="43"/>[m
[32m+[m[32m            line="1067"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        mNativeInterface.disconnect(device);"[m
[31m-        errorLine2="                                         ~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java"[m
[31m-            line="1114"[m
[31m-            column="42"/>[m
[32m+[m[32m            line="1114"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        mNativeInterface.disconnect(device);"[m
[31m-        errorLine2="                                         ~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java"[m
[31m-            line="1264"[m
[31m-            column="42"/>[m
[32m+[m[32m            line="1264"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        if (mNativeInterface.sendATCmd(mCurrentDevice,"[m
[31m-        errorLine2="                                             ~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java"[m
[31m-            line="1272"[m
[31m-            column="46"/>[m
[32m+[m[32m            line="1272"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mNativeInterface.connect(device);"[m
[31m-        errorLine2="                                     ~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java"[m
[31m-            line="1382"[m
[31m-            column="38"/>[m
[32m+[m[32m            line="1382"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    if (!mNativeInterface.disconnect(dev)) {"[m
[31m-        errorLine2="                                          ~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java"[m
[31m-            line="1389"[m
[31m-            column="43"/>[m
[32m+[m[32m            line="1389"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    if (!mNativeInterface.connectAudio(mCurrentDevice)) {"[m
[31m-        errorLine2="                                          ~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java"[m
[31m-            line="1395"[m
[31m-            column="43"/>[m
[32m+[m[32m            line="1395"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        if (mNativeInterface.startVoiceRecognition(mCurrentDevice)) {"[m
[31m-        errorLine2="                                             ~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java"[m
[31m-            line="1414"[m
[31m-            column="46"/>[m
[32m+[m[32m            line="1414"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        if (mNativeInterface.stopVoiceRecognition(mCurrentDevice)) {"[m
[31m-        errorLine2="                                             ~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java"[m
[31m-            line="1424"[m
[31m-            column="46"/>[m
[32m+[m[32m            line="1424"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        mNativeInterface.sendATCmd(mCurrentDevice,"[m
[31m-        errorLine2="                                         ~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java"[m
[31m-            line="1444"[m
[31m-            column="42"/>[m
[32m+[m[32m            line="1444"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        if (mNativeInterface.setVolume(mCurrentDevice,"[m
[31m-        errorLine2="                                             ~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java"[m
[31m-            line="1464"[m
[31m-            column="46"/>[m
[32m+[m[32m            line="1464"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    if (mNativeInterface.dial(mCurrentDevice, c.getNumber())) {"[m
[31m-        errorLine2="                                         ~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java"[m
[31m-            line="1475"[m
[31m-            column="42"/>[m
[32m+[m[32m            line="1475"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    if (mNativeInterface.sendDtmf(mCurrentDevice,"[m
[31m-        errorLine2="                                         ~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java"[m
[31m-            line="1507"[m
[31m-            column="42"/>[m
[32m+[m[32m            line="1507"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    if (mNativeInterface.retrieveSubscriberInfo(mCurrentDevice)) {"[m
[31m-        errorLine2="                                         ~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java"[m
[31m-            line="1515"[m
[31m-            column="42"/>[m
[32m+[m[32m            line="1515"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                                if (mNativeInterface.queryCurrentOperatorName(mCurrentDevice)) {"[m
[31m-        errorLine2="                                                     ~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java"[m
[31m-            line="1576"[m
[31m-            column="54"/>[m
[32m+[m[32m            line="1576"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (!mNativeInterface.sendAndroidAt(mCurrentDevice,"[m
[31m-        errorLine2="                              ~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java"[m
[31m-            line="2296"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="2296"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (!mNativeInterface.sendAndroidAt(mCurrentDevice, &quot;+ANDROID=?&quot;)) {"[m
[31m-        errorLine2="                              ~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java"[m
[31m-            line="2306"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="2306"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        return HeadsetStateMachine.make(device, looper, headsetService, adapterService,"[m
[31m-        errorLine2="                                   ~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetObjectsFactory.java"[m
[31m-            line="76"[m
[31m-            column="36"/>[m
[32m+[m[32m            line="76"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        HeadsetStateMachine.destroy(stateMachine);"[m
[31m-        errorLine2="                            ~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetObjectsFactory.java"[m
[31m-            line="86"[m
[31m-            column="29"/>[m
[32m+[m[32m            line="86"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        return new HeadsetSystemInterface(service);"[m
[31m-        errorLine2="               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetObjectsFactory.java"[m
[31m-            line="96"[m
[31m-            column="16"/>[m
[32m+[m[32m            line="96"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            mHandler = new Handler(headsetService.getStateMachinesThreadLooper());"[m
[31m-        errorLine2="                                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetPhoneState.java"[m
[31m-            line="89"[m
[31m-            column="51"/>[m
[32m+[m[32m            line="89"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mDatabaseManager = Objects.requireNonNull(mAdapterService.getDatabase(),"[m
[31m-        errorLine2="                                                                  ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="181"[m
[31m-            column="67"/>[m
[32m+[m[32m            line="183"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mNativeInterface.init(mMaxHeadsetConnections + 1, isInbandRingingEnabled());"[m
[31m-        errorLine2="                         ~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="193"[m
[31m-            column="26"/>[m
[32m+[m[32m            line="195"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                if (mSystemInterface.getVoiceRecognitionWakeLock().isHeld()) {"[m
[31m-        errorLine2="                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="254"[m
[31m-            column="38"/>[m
[32m+[m[32m            line="256"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mSystemInterface.getVoiceRecognitionWakeLock().release();"[m
[31m-        errorLine2="                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="255"[m
[31m-            column="38"/>[m
[32m+[m[32m            line="257"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mNativeInterface.cleanup();"[m
[31m-        errorLine2="                         ~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="265"[m
[31m-            column="26"/>[m
[32m+[m[32m            line="267"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mSystemInterface.stop();"[m
[31m-        errorLine2="                         ~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="268"[m
[31m-            column="26"/>[m
[32m+[m[32m            line="270"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                stateMachine -> stateMachine.sendMessage(HeadsetStateMachine.DEVICE_STATE_CHANGED,"[m
[31m-        errorLine2="                                                                             ~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="351"[m
[31m-            column="78"/>[m
[32m+[m[32m            line="353"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            stateMachine.sendMessage(HeadsetStateMachine.STACK_EVENT, stackEvent);"[m
[31m-        errorLine2="                                                         ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="386"[m
[31m-            column="58"/>[m
[32m+[m[32m            line="388"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mSystemInterface.getHeadsetPhoneState().setCindBatteryCharge(cindBatteryLevel);"[m
[31m-        errorLine2="                                     ~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="408"[m
[31m-            column="38"/>[m
[32m+[m[32m            line="410"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                                HeadsetStateMachine.INTENT_SCO_VOLUME_CHANGED, intent));"[m
[31m-        errorLine2="                                                    ~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="415"[m
[31m-            column="53"/>[m
[32m+[m[32m            line="417"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                                    HeadsetStateMachine.INTENT_CONNECTION_ACCESS_REPLY, intent);"[m
[31m-        errorLine2="                                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="434"[m
[31m-            column="57"/>[m
[32m+[m[32m            line="436"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        if (stateMachine.getConnectionState()"[m
[31m-        errorLine2="                                         ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="454"[m
[31m-            column="42"/>[m
[32m+[m[32m            line="456"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        ParcelUuid[] featureUuids = mAdapterService.getRemoteUuids(device);"[m
[31m-        errorLine2="                                                    ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="928"[m
[31m-            column="53"/>[m
[32m+[m[32m            line="930"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            int connectionState = stateMachine.getConnectionState();"[m
[31m-        errorLine2="                                               ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="943"[m
[31m-            column="48"/>[m
[32m+[m[32m            line="945"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            stateMachine.sendMessage(HeadsetStateMachine.CONNECT, device);"[m
[31m-        errorLine2="                                                         ~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="968"[m
[31m-            column="58"/>[m
[32m+[m[32m            line="970"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            int connectionState = stateMachine.getConnectionState();"[m
[31m-        errorLine2="                                               ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="987"[m
[31m-            column="48"/>[m
[32m+[m[32m            line="989"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            stateMachine.sendMessage(HeadsetStateMachine.DISCONNECT, device);"[m
[31m-        errorLine2="                                                         ~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="994"[m
[31m-            column="58"/>[m
[32m+[m[32m            line="996"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                if (stateMachine.getConnectionState() == BluetoothProfile.STATE_CONNECTED) {"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1003"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="1005"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    devices.add(stateMachine.getDevice());"[m
[31m-        errorLine2="                                             ~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1004"[m
[31m-            column="46"/>[m
[32m+[m[32m            line="1006"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            return stateMachine.getConnectionState();"[m
[31m-        errorLine2="                                ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1051"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="1053"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (!mDatabaseManager.setProfileConnectionPolicy(device, BluetoothProfile.HEADSET,"[m
[31m-        errorLine2="                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1075"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="1077"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                .getProfileConnectionPolicy(device, BluetoothProfile.HEADSET);"[m
[31m-        errorLine2="                 ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1101"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="1103"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                if (mSystemInterface.getVoiceRecognitionWakeLock().isHeld()) {"[m
[31m-        errorLine2="                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1155"[m
[31m-            column="38"/>[m
[32m+[m[32m            line="1157"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mSystemInterface.getVoiceRecognitionWakeLock().release();"[m
[31m-        errorLine2="                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1156"[m
[31m-            column="38"/>[m
[32m+[m[32m            line="1158"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            int connectionState = stateMachine.getConnectionState();"[m
[31m-        errorLine2="                                               ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1169"[m
[31m-            column="48"/>[m
[32m+[m[32m            line="1171"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                stateMachine.sendMessage(HeadsetStateMachine.VOICE_RECOGNITION_RESULT,"[m
[31m-        errorLine2="                                                             ~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1177"[m
[31m-            column="62"/>[m
[32m+[m[32m            line="1179"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                stateMachine.sendMessage(HeadsetStateMachine.VOICE_RECOGNITION_START, device);"[m
[31m-        errorLine2="                                                             ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1180"[m
[31m-            column="62"/>[m
[32m+[m[32m            line="1182"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            stateMachine.sendMessage(HeadsetStateMachine.CONNECT_AUDIO, device);"[m
[31m-        errorLine2="                                                         ~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1182"[m
[31m-            column="58"/>[m
[32m+[m[32m            line="1184"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            int connectionState = stateMachine.getConnectionState();"[m
[31m-        errorLine2="                                               ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1200"[m
[31m-            column="48"/>[m
[32m+[m[32m            line="1202"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            stateMachine.sendMessage(HeadsetStateMachine.VOICE_RECOGNITION_STOP, device);"[m
[31m-        errorLine2="                                                         ~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1211"[m
[31m-            column="58"/>[m
[32m+[m[32m            line="1213"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            stateMachine.sendMessage(HeadsetStateMachine.DISCONNECT_AUDIO, device);"[m
[31m-        errorLine2="                                                         ~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1212"[m
[31m-            column="58"/>[m
[32m+[m[32m            line="1214"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            return stateMachine.getAudioState() == BluetoothHeadset.STATE_AUDIO_CONNECTED;"[m
[31m-        errorLine2="                                ~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1227"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="1229"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            return stateMachine.getAudioState();"[m
[31m-        errorLine2="                                ~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1237"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="1239"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mNativeInterface.setScoAllowed(allowed);"[m
[31m-        errorLine2="                         ~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1244"[m
[31m-            column="26"/>[m
[32m+[m[32m            line="1246"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            return stateMachine.getHfpCallAudioPolicy();"[m
[31m-        errorLine2="                                ~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1333"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="1335"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            if (!mNativeInterface.setActiveDevice(device)) {"[m
[31m-        errorLine2="                                  ~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1400"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="1402"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        mNativeInterface.setActiveDevice(previousActiveDevice);"[m
[31m-        errorLine2="                                         ~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1415"[m
[31m-            column="42"/>[m
[32m+[m[32m            line="1417"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        mNativeInterface.setActiveDevice(previousActiveDevice);"[m
[31m-        errorLine2="                                         ~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1440"[m
[31m-            column="42"/>[m
[32m+[m[32m            line="1442"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            if (stateMachine.getConnectionState() != BluetoothProfile.STATE_CONNECTED) {"[m
[31m-        errorLine2="                             ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1486"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="1488"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            if (stateMachine.getAudioState() != BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {"[m
[31m-        errorLine2="                             ~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1490"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="1492"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            stateMachine.sendMessage(HeadsetStateMachine.CONNECT_AUDIO, device);"[m
[31m-        errorLine2="                                                         ~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1499"[m
[31m-            column="58"/>[m
[32m+[m[32m            line="1501"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                if (stateMachine.getAudioState() != BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1508"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="1510"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    devices.add(stateMachine.getDevice());"[m
[31m-        errorLine2="                                             ~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1509"[m
[31m-            column="46"/>[m
[32m+[m[32m            line="1511"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            if (stateMachine.getAudioState() == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {"[m
[31m-        errorLine2="                             ~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1541"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="1543"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            stateMachine.sendMessage(HeadsetStateMachine.DISCONNECT_AUDIO, device);"[m
[31m-        errorLine2="                                                         ~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1545"[m
[31m-            column="58"/>[m
[32m+[m[32m            line="1547"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        HeadsetStateMachine.DIALING_OUT_RESULT, 0 /* fail */, 0,"[m
[31m-        errorLine2="                                            ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1624"[m
[31m-            column="45"/>[m
[32m+[m[32m            line="1626"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                if (mSystemInterface.getVoiceRecognitionWakeLock().isHeld()) {"[m
[31m-        errorLine2="                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1698"[m
[31m-            column="38"/>[m
[32m+[m[32m            line="1700"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mSystemInterface.getVoiceRecognitionWakeLock().release();"[m
[31m-        errorLine2="                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1699"[m
[31m-            column="38"/>[m
[32m+[m[32m            line="1701"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        HeadsetStateMachine.VOICE_RECOGNITION_RESULT, 0 /* fail */, 0,"[m
[31m-        errorLine2="                                            ~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1703"[m
[31m-            column="45"/>[m
[32m+[m[32m            line="1705"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            if (!mSystemInterface.activateVoiceRecognition()) {"[m
[31m-        errorLine2="                                  ~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1756"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="1758"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            if (!mSystemInterface.getVoiceRecognitionWakeLock().isHeld()) {"[m
[31m-        errorLine2="                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1764"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="1766"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mSystemInterface.getVoiceRecognitionWakeLock().acquire(sStartVrTimeoutMs);"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1765"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="1767"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                if (mSystemInterface.getVoiceRecognitionWakeLock().isHeld()) {"[m
[31m-        errorLine2="                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1785"[m
[31m-            column="38"/>[m
[32m+[m[32m            line="1787"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mSystemInterface.getVoiceRecognitionWakeLock().release();"[m
[31m-        errorLine2="                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1786"[m
[31m-            column="38"/>[m
[32m+[m[32m            line="1788"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            if (!mSystemInterface.deactivateVoiceRecognition()) {"[m
[31m-        errorLine2="                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1801"[m
[31m-            column="35"/>[m
[32m+[m[32m            line="1803"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                                    HeadsetStateMachine.DIALING_OUT_RESULT, 1 /* success */, 0,"[m
[31m-        errorLine2="                                                        ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1832"[m
[31m-            column="57"/>[m
[32m+[m[32m            line="1834"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            boolean isCallIdleBefore = mSystemInterface.isCallIdle();"[m
[31m-        errorLine2="                                                        ~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1845"[m
[31m-            column="57"/>[m
[32m+[m[32m            line="1847"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            mSystemInterface.getHeadsetPhoneState().setNumActiveCall(numActive);"[m
[31m-        errorLine2="                             ~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1846"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="1848"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            mSystemInterface.getHeadsetPhoneState().setNumHeldCall(numHeld);"[m
[31m-        errorLine2="                             ~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1847"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="1849"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            mSystemInterface.getHeadsetPhoneState().setCallState(callState);"[m
[31m-        errorLine2="                             ~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1848"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="1850"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    &amp;&amp; !mSystemInterface.isCallIdle() &amp;&amp; isCallIdleBefore) {"[m
[31m-        errorLine2="                                         ~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1851"[m
[31m-            column="42"/>[m
[32m+[m[32m            line="1853"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mSystemInterface.getAudioManager().setA2dpSuspended(true);"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1852"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="1854"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mSystemInterface.getAudioManager().setLeAudioSuspended(true);"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1853"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="1855"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                stateMachine -> stateMachine.sendMessage(HeadsetStateMachine.CALL_STATE_CHANGED,"[m
[31m-        errorLine2="                                                                             ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1857"[m
[31m-            column="78"/>[m
[32m+[m[32m            line="1859"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    &amp;&amp; mSystemInterface.isCallIdle() &amp;&amp; !isAudioOn()) {"[m
[31m-        errorLine2="                                        ~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1861"[m
[31m-            column="41"/>[m
[32m+[m[32m            line="1863"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mSystemInterface.getAudioManager().setA2dpSuspended(false);"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1863"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="1865"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mSystemInterface.getAudioManager().setLeAudioSuspended(false);"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1864"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="1866"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                BluetoothSinkAudioPolicy currentPolicy = stateMachine.getHfpCallAudioPolicy();"[m
[31m-        errorLine2="                                                                      ~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1872"[m
[31m-            column="71"/>[m
[32m+[m[32m            line="1874"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                stateMachine -> stateMachine.sendMessage(HeadsetStateMachine.SEND_CLCC_RESPONSE,"[m
[31m-        errorLine2="                                                                             ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1892"[m
[31m-            column="78"/>[m
[32m+[m[32m            line="1894"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            int connectionState = stateMachine.getConnectionState();"[m
[31m-        errorLine2="                                               ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1910"[m
[31m-            column="48"/>[m
[32m+[m[32m            line="1912"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            stateMachine.sendMessage(HeadsetStateMachine.SEND_VENDOR_SPECIFIC_RESULT_CODE,"[m
[31m-        errorLine2="                                                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="1919"[m
[31m-            column="58"/>[m
[32m+[m[32m            line="1921"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                            .sendMessage(HeadsetStateMachine.SEND_BSIR, enabled);"[m
[31m-        errorLine2="                                                             ~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="2013"[m
[31m-            column="62"/>[m
[32m+[m[32m            line="2015"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            if (mVoiceRecognitionStarted || mVirtualCallStarted || !mSystemInterface.isCallIdle()) {"[m
[31m-        errorLine2="                                                                                     ~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="2031"[m
[31m-            column="86"/>[m
[32m+[m[32m            line="2033"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        + &quot;, isCallIdle=&quot; + mSystemInterface.isCallIdle());"[m
[31m-        errorLine2="                                                             ~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="2034"[m
[31m-            column="62"/>[m
[32m+[m[32m            line="2036"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        return mSystemInterface.isInCall() || (mSystemInterface.isRinging()"[m
[31m-        errorLine2="                                ~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="2042"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="2044"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        return mSystemInterface.isInCall() || (mSystemInterface.isRinging()"[m
[31m-        errorLine2="                                                                ~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="2042"[m
[31m-            column="65"/>[m
[32m+[m[32m            line="2044"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        int bondState = mAdapterService.getBondState(device);"[m
[31m-        errorLine2="                                        ~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="2133"[m
[31m-            column="41"/>[m
[32m+[m[32m            line="2135"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within package private scope"[m
[31m-        errorLine1="                if (a2dpService != null &amp;&amp; a2dpService.okToConnect(device, true)) {"[m
[31m-        errorLine2="                                                       ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within package private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="2144"[m
[31m-            column="56"/>[m
[32m+[m[32m            line="2146"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            Log.w(TAG, &quot;isScoAcceptable: rejected SCO, inCall=&quot; + mSystemInterface.isInCall()"[m
[31m-        errorLine2="                                                                                   ~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="2192"[m
[31m-            column="84"/>[m
[32m+[m[32m            line="2199"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    + mSystemInterface.isRinging() + &quot;, inbandRinging=&quot; + isInbandRingingEnabled()"[m
[31m-        errorLine2="                                       ~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="2194"[m
[31m-            column="40"/>[m
[32m+[m[32m            line="2201"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        DatabaseManager dbManager = mAdapterService.getDatabase();"[m
[31m-        errorLine2="                                                    ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="2228"[m
[31m-            column="53"/>[m
[32m+[m[32m            line="2235"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        boolean isScoOn = mSystemInterface.getAudioManager().isBluetoothScoOn();"[m
[31m-        errorLine2="                                           ~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="2258"[m
[31m-            column="44"/>[m
[32m+[m[32m            line="2265"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            ProfileService.println(sb, &quot;Telecom.isInCall(): &quot; + mSystemInterface.isInCall());"[m
[31m-        errorLine2="                                                                                 ~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="2282"[m
[31m-            column="82"/>[m
[32m+[m[32m            line="2289"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            ProfileService.println(sb, &quot;Telecom.isRinging(): &quot; + mSystemInterface.isRinging());"[m
[31m-        errorLine2="                                                                                  ~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="2283"[m
[31m-            column="83"/>[m
[32m+[m[32m            line="2290"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        &quot;==== StateMachine for &quot; + stateMachine.getDevice() + &quot; ====&quot;);"[m
[31m-        errorLine2="                                                                ~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="2286"[m
[31m-            column="65"/>[m
[32m+[m[32m            line="2293"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                stateMachine.dump(sb);"[m
[31m-        errorLine2="                             ~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetService.java"[m
[31m-            line="2287"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="2294"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mDatabaseManager = Objects.requireNonNull(mAdapterService.getDatabase(),"[m
[31m-        errorLine2="                                                                  ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hearingaid/HearingAidService.java"[m
[31m-            line="136"[m
[31m-            column="67"/>[m
[32m+[m[32m            line="136"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        ParcelUuid[] featureUuids = mAdapterService.getRemoteUuids(device);"[m
[31m-        errorLine2="                                                    ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hearingaid/HearingAidService.java"[m
[31m-            line="283"[m
[31m-            column="53"/>[m
[32m+[m[32m            line="283"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (!mDatabaseManager.setProfileConnectionPolicy(device, BluetoothProfile.HEARING_AID,"[m
[31m-        errorLine2="                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hearingaid/HearingAidService.java"[m
[31m-            line="530"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="530"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                .getProfileConnectionPolicy(device, BluetoothProfile.HEARING_AID);"[m
[31m-        errorLine2="                 ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hearingaid/HearingAidService.java"[m
[31m-            line="559"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="559"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            sm.sendMessage(HearingAidStateMachine.STACK_EVENT, stackEvent);"[m
[31m-        errorLine2="                                                  ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hearingaid/HearingAidService.java"[m
[31m-            line="731"[m
[31m-            column="51"/>[m
[32m+[m[32m            line="731"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            int bondState = mAdapterService.getBondState(device);"[m
[31m-        errorLine2="                                            ~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hearingaid/HearingAidService.java"[m
[31m-            line="959"[m
[31m-            column="45"/>[m
[32m+[m[32m            line="959"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (!mDatabaseManager.setProfileConnectionPolicy(device, BluetoothProfile.HID_DEVICE,"[m
[31m-        errorLine2="                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hid/HidDeviceService.java"[m
[31m-            line="714"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="714"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                .getProfileConnectionPolicy(device, BluetoothProfile.HID_DEVICE);"[m
[31m-        errorLine2="                 ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hid/HidDeviceService.java"[m
[31m-            line="744"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="744"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mDatabaseManager = Objects.requireNonNull(AdapterService.getAdapterService().getDatabase(),"[m
[31m-        errorLine2="                                                                                     ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hid/HidDeviceService.java"[m
[31m-            line="770"[m
[31m-            column="86"/>[m
[32m+[m[32m            line="770"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mDatabaseManager = Objects.requireNonNull(AdapterService.getAdapterService().getDatabase(),"[m
[31m-        errorLine2="                                                                                     ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hid/HidHostService.java"[m
[31m-            line="103"[m
[31m-            column="86"/>[m
[32m+[m[32m            line="103"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (!mDatabaseManager.setProfileConnectionPolicy(device, BluetoothProfile.HID_HOST,"[m
[31m-        errorLine2="                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hid/HidHostService.java"[m
[31m-            line="674"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="674"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                .getProfileConnectionPolicy(device, BluetoothProfile.HID_HOST);"[m
[31m-        errorLine2="                 ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hid/HidHostService.java"[m
[31m-            line="706"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="706"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        return new LeAudioTmapGattServer("[m
[31m-        errorLine2="               ^">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/le_audio/LeAudioObjectsFactory.java"[m
[31m-            line="70"[m
[31m-            column="16"/>[m
[32m+[m[32m            line="70"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                new LeAudioTmapGattServer.BluetoothGattServerProxy(context));"[m
[31m-        errorLine2="                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/le_audio/LeAudioObjectsFactory.java"[m
[31m-            line="71"[m
[31m-            column="17"/>[m
[32m+[m[32m            line="71"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            mTmapGattServer.start(mTmapRoleMask);"[m
[31m-        errorLine2="                            ~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/le_audio/LeAudioService.java"[m
[31m-            line="245"[m
[31m-            column="29"/>[m
[32m+[m[32m            line="245"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mDatabaseManager = Objects.requireNonNull(mAdapterService.getDatabase(),"[m
[31m-        errorLine2="                                                                  ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/le_audio/LeAudioService.java"[m
[31m-            line="266"[m
[31m-            column="67"/>[m
[32m+[m[32m            line="266"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                LeAudioTmapGattServer.TMAP_ROLE_FLAG_CG | LeAudioTmapGattServer.TMAP_ROLE_FLAG_UMS;"[m
[31m-        errorLine2="                                      ~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/le_audio/LeAudioService.java"[m
[31m-            line="296"[m
[31m-            column="39"/>[m
[32m+[m[32m            line="296"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                LeAudioTmapGattServer.TMAP_ROLE_FLAG_CG | LeAudioTmapGattServer.TMAP_ROLE_FLAG_UMS;"[m
[31m-        errorLine2="                                                                                ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/le_audio/LeAudioService.java"[m
[31m-            line="296"[m
[31m-            column="81"/>[m
[32m+[m[32m            line="296"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            mTmapRoleMask |= LeAudioTmapGattServer.TMAP_ROLE_FLAG_BMS;"[m
[31m-        errorLine2="                                                   ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/le_audio/LeAudioService.java"[m
[31m-            line="307"[m
[31m-            column="52"/>[m
[32m+[m[32m            line="307"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            mTmapGattServer.stop();"[m
[31m-        errorLine2="                            ~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/le_audio/LeAudioService.java"[m
[31m-            line="360"[m
[31m-            column="29"/>[m
[32m+[m[32m            line="360"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        ParcelUuid[] featureUuids = mAdapterService.getRemoteUuids(device);"[m
[31m-        errorLine2="                                                    ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/le_audio/LeAudioService.java"[m
[31m-            line="534"[m
[31m-            column="53"/>[m
[32m+[m[32m            line="534"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                sm.sendMessage(LeAudioStateMachine.STACK_EVENT, stackEvent);"[m
[31m-        errorLine2="                                                   ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/le_audio/LeAudioService.java"[m
[31m-            line="1561"[m
[31m-            column="52"/>[m
[32m+[m[32m            line="1561"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                                            == mAdapterService.getBondState(device));"[m
[31m-        errorLine2="                                                               ~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/le_audio/LeAudioService.java"[m
[31m-            line="1814"[m
[31m-            column="64"/>[m
[32m+[m[32m            line="1814"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                sm.sendMessage(LeAudioStateMachine.STACK_EVENT, stackEvent);"[m
[31m-        errorLine2="                                                   ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/le_audio/LeAudioService.java"[m
[31m-            line="1860"[m
[31m-            column="52"/>[m
[32m+[m[32m            line="1860"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        int bondState = mAdapterService.getBondState(device);"[m
[31m-        errorLine2="                                        ~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/le_audio/LeAudioService.java"[m
[31m-            line="2249"[m
[31m-            column="41"/>[m
[32m+[m[32m            line="2249"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        int bondState = mAdapterService.getBondState(device);"[m
[31m-        errorLine2="                                        ~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/le_audio/LeAudioService.java"[m
[31m-            line="2319"[m
[31m-            column="41"/>[m
[32m+[m[32m            line="2319"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (!mDatabaseManager.setProfileConnectionPolicy(device, BluetoothProfile.LE_AUDIO,"[m
[31m-        errorLine2="                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/le_audio/LeAudioService.java"[m
[31m-            line="2442"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="2442"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                .getProfileConnectionPolicy(device, BluetoothProfile.LE_AUDIO);"[m
[31m-        errorLine2="                 ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/le_audio/LeAudioService.java"[m
[31m-            line="2506"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="2506"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                &amp;&amp; userUuid.getUuid() != TbsGatt.UUID_GTBS) {"[m
[31m-        errorLine2="                                                 ~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/le_audio/LeAudioService.java"[m
[31m-            line="2544"[m
[31m-            column="50"/>[m
[32m+[m[32m            line="2544"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (!mDatabaseManager.setProfileConnectionPolicy(device, BluetoothProfile.MAP_CLIENT,"[m
[31m-        errorLine2="                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/mapclient/MapClientService.java"[m
[31m-            line="261"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="261"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                .getProfileConnectionPolicy(device, BluetoothProfile.MAP_CLIENT);"[m
[31m-        errorLine2="                 ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/mapclient/MapClientService.java"[m
[31m-            line="290"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="290"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mDatabaseManager = Objects.requireNonNull(AdapterService.getAdapterService().getDatabase(),"[m
[31m-        errorLine2="                                                                                     ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/mapclient/MapClientService.java"[m
[31m-            line="310"[m
[31m-            column="86"/>[m
[32m+[m[32m            line="310"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            mService.cleanupDevice(mDevice);"[m
[31m-        errorLine2="                     ~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/mapclient/MceStateMachine.java"[m
[31m-            line="246"[m
[31m-            column="22"/>[m
[32m+[m[32m            line="246"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mDatabaseManager = Objects.requireNonNull(AdapterService.getAdapterService().getDatabase(),"[m
[31m-        errorLine2="                                                                                     ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/pan/PanService.java"[m
[31m-            line="152"[m
[31m-            column="86"/>[m
[32m+[m[32m            line="152"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (!mDatabaseManager.setProfileConnectionPolicy(device, BluetoothProfile.PAN,"[m
[31m-        errorLine2="                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/pan/PanService.java"[m
[31m-            line="536"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="536"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                .getProfileConnectionPolicy(device, BluetoothProfile.PAN);"[m
[31m-        errorLine2="                 ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/pan/PanService.java"[m
[31m-            line="562"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="562"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mDatabaseManager = Objects.requireNonNull(AdapterService.getAdapterService().getDatabase(),"[m
[31m-        errorLine2="                                                                                     ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/pbapclient/PbapClientService.java"[m
[31m-            line="133"[m
[31m-            column="86"/>[m
[32m+[m[32m            line="133"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (!mDatabaseManager.setProfileConnectionPolicy(device, BluetoothProfile.PBAP_CLIENT,"[m
[31m-        errorLine2="                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/pbapclient/PbapClientService.java"[m
[31m-            line="612"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="612"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                .getProfileConnectionPolicy(device, BluetoothProfile.PBAP_CLIENT);"[m
[31m-        errorLine2="                 ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/pbapclient/PbapClientService.java"[m
[31m-            line="644"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="644"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mDatabaseManager = Objects.requireNonNull(mAdapterService.getDatabase(),"[m
[31m-        errorLine2="                                                                  ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="273"[m
[31m-            column="67"/>[m
[32m+[m[32m            line="273"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="315"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="315"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="315"[m
[31m-            column="47"/>[m
[32m+[m[32m            line="315"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="328"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="328"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="328"[m
[31m-            column="47"/>[m
[32m+[m[32m            line="328"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                    ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="335"[m
[31m-            column="37"/>[m
[32m+[m[32m            line="335"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="335"[m
[31m-            column="51"/>[m
[32m+[m[32m            line="335"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="349"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="349"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="349"[m
[31m-            column="47"/>[m
[32m+[m[32m            line="349"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                    ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="356"[m
[31m-            column="37"/>[m
[32m+[m[32m            line="356"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="356"[m
[31m-            column="51"/>[m
[32m+[m[32m            line="356"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="372"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="372"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="372"[m
[31m-            column="47"/>[m
[32m+[m[32m            line="372"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="388"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="388"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="388"[m
[31m-            column="47"/>[m
[32m+[m[32m            line="388"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                    ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="403"[m
[31m-            column="37"/>[m
[32m+[m[32m            line="403"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="403"[m
[31m-            column="51"/>[m
[32m+[m[32m            line="403"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="408"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="408"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="408"[m
[31m-            column="47"/>[m
[32m+[m[32m            line="408"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="419"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="419"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="419"[m
[31m-            column="47"/>[m
[32m+[m[32m            line="419"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                    ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="427"[m
[31m-            column="37"/>[m
[32m+[m[32m            line="427"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="427"[m
[31m-            column="51"/>[m
[32m+[m[32m            line="427"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                    ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="444"[m
[31m-            column="37"/>[m
[32m+[m[32m            line="444"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="444"[m
[31m-            column="51"/>[m
[32m+[m[32m            line="444"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="450"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="450"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="450"[m
[31m-            column="47"/>[m
[32m+[m[32m            line="450"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                    ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="466"[m
[31m-            column="37"/>[m
[32m+[m[32m            line="466"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="466"[m
[31m-            column="51"/>[m
[32m+[m[32m            line="466"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="471"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="471"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="471"[m
[31m-            column="47"/>[m
[32m+[m[32m            line="471"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="485"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="485"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="485"[m
[31m-            column="47"/>[m
[32m+[m[32m            line="485"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="500"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="500"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                mAdapterService.getDatabase().setProfileConnectionPolicy(device,"[m
[31m-        errorLine2="                                              ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/PhonePolicy.java"[m
[31m-            line="500"[m
[31m-            column="47"/>[m
[32m+[m[32m            line="500"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        AdapterService.getAdapterService().getDatabase()"[m
[31m-        errorLine2="                                           ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/sap/SapService.java"[m
[31m-            line="645"[m
[31m-            column="44"/>[m
[32m+[m[32m            line="645"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                .setProfileConnectionPolicy(device, BluetoothProfile.SAP, connectionPolicy);"[m
[31m-        errorLine2="                 ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/sap/SapService.java"[m
[31m-            line="646"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="646"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        return AdapterService.getAdapterService().getDatabase()"[m
[31m-        errorLine2="                                                  ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/sap/SapService.java"[m
[31m-            line="669"[m
[31m-            column="51"/>[m
[32m+[m[32m            line="669"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                .getProfileConnectionPolicy(device, BluetoothProfile.SAP);"[m
[31m-        errorLine2="                 ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/sap/SapService.java"[m
[31m-            line="670"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="670"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            a2dpService.setSilenceMode(device, state);"[m
[31m-        errorLine2="                        ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/SilenceDeviceManager.java"[m
[31m-            line="273"[m
[31m-            column="25"/>[m
[32m+[m[32m            line="273"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            headsetService.setSilenceMode(device, state);"[m
[31m-        errorLine2="                           ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/SilenceDeviceManager.java"[m
[31m-            line="277"[m
[31m-            column="28"/>[m
[32m+[m[32m            line="277"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        int ccid = ContentControlIdKeeper.acquireCcid(new ParcelUuid(TbsGatt.UUID_GTBS),"[m
[31m-        errorLine2="                                                                             ~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="164"[m
[31m-            column="78"/>[m
[32m+[m[32m            line="164"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (request.requestedOpcode == TbsGatt.CALL_CONTROL_POINT_OPCODE_TERMINATE) {"[m
[31m-        errorLine2="                                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="407"[m
[31m-            column="48"/>[m
[32m+[m[32m            line="407"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                result = TbsGatt.CALL_CONTROL_POINT_RESULT_SUCCESS;"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="409"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="409"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                result = TbsGatt.CALL_CONTROL_POINT_RESULT_OPERATION_NOT_POSSIBLE;"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="411"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="411"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        } else if (request.requestedOpcode == TbsGatt.CALL_CONTROL_POINT_OPCODE_ACCEPT) {"[m
[31m-        errorLine2="                                                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="413"[m
[31m-            column="55"/>[m
[32m+[m[32m            line="413"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                result = TbsGatt.CALL_CONTROL_POINT_RESULT_SUCCESS;"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="415"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="415"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                result = TbsGatt.CALL_CONTROL_POINT_RESULT_OPERATION_NOT_POSSIBLE;"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="417"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="417"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        } else if (request.requestedOpcode == TbsGatt.CALL_CONTROL_POINT_OPCODE_LOCAL_HOLD) {"[m
[31m-        errorLine2="                                                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="419"[m
[31m-            column="55"/>[m
[32m+[m[32m            line="419"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                result = TbsGatt.CALL_CONTROL_POINT_RESULT_SUCCESS;"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="422"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="422"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                result = TbsGatt.CALL_CONTROL_POINT_RESULT_OPERATION_NOT_POSSIBLE;"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="424"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="424"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        } else if (request.requestedOpcode == TbsGatt.CALL_CONTROL_POINT_OPCODE_LOCAL_RETRIEVE) {"[m
[31m-        errorLine2="                                                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="426"[m
[31m-            column="55"/>[m
[32m+[m[32m            line="426"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                result = TbsGatt.CALL_CONTROL_POINT_RESULT_SUCCESS;"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="429"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="429"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                result = TbsGatt.CALL_CONTROL_POINT_RESULT_OPERATION_NOT_POSSIBLE;"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="431"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="431"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        } else if (request.requestedOpcode == TbsGatt.CALL_CONTROL_POINT_OPCODE_ORIGINATE) {"[m
[31m-        errorLine2="                                                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="433"[m
[31m-            column="55"/>[m
[32m+[m[32m            line="433"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                result = TbsGatt.CALL_CONTROL_POINT_RESULT_SUCCESS;"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="435"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="435"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                result = TbsGatt.CALL_CONTROL_POINT_RESULT_OPERATION_NOT_POSSIBLE;"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="437"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="437"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        } else if (request.requestedOpcode == TbsGatt.CALL_CONTROL_POINT_OPCODE_JOIN) {"[m
[31m-        errorLine2="                                                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="439"[m
[31m-            column="55"/>[m
[32m+[m[32m            line="439"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                result = TbsGatt.CALL_CONTROL_POINT_RESULT_OPERATION_NOT_POSSIBLE;"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="444"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="444"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                result = TbsGatt.CALL_CONTROL_POINT_RESULT_SUCCESS;"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="456"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="456"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            result = TbsGatt.CALL_CONTROL_POINT_RESULT_OPERATION_NOT_POSSIBLE;"[m
[31m-        errorLine2="                             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="459"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="459"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            return TbsGatt.CALL_CONTROL_POINT_RESULT_INVALID_CALL_INDEX;"[m
[31m-        errorLine2="                           ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="471"[m
[31m-            column="28"/>[m
[32m+[m[32m            line="471"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                &amp;&amp; requestedOpcode == TbsGatt.CALL_CONTROL_POINT_OPCODE_ORIGINATE) {"[m
[31m-        errorLine2="                                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="475"[m
[31m-            column="47"/>[m
[32m+[m[32m            line="475"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            return TbsGatt.CALL_CONTROL_POINT_RESULT_INVALID_OUTGOING_URI;"[m
[31m-        errorLine2="                           ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="476"[m
[31m-            column="28"/>[m
[32m+[m[32m            line="476"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        return TbsGatt.CALL_CONTROL_POINT_RESULT_OPERATION_NOT_POSSIBLE;"[m
[31m-        errorLine2="                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="479"[m
[31m-            column="24"/>[m
[32m+[m[32m            line="479"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            mTbsGatt.setCallControlPointResult(device, TbsGatt.CALL_CONTROL_POINT_OPCODE_ORIGINATE,"[m
[31m-        errorLine2="                                                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="753"[m
[31m-            column="64"/>[m
[32m+[m[32m            line="753"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    TbsCall.INDEX_UNASSIGNED, TbsGatt.CALL_CONTROL_POINT_RESULT_SUCCESS);"[m
[31m-        errorLine2="                                                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="754"[m
[31m-            column="55"/>[m
[32m+[m[32m            line="754"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    TbsGatt.CALL_CONTROL_POINT_OPCODE_ORIGINATE, TbsCall.INDEX_UNASSIGNED);"[m
[31m-        errorLine2="                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="759"[m
[31m-            column="29"/>[m
[32m+[m[32m            line="759"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                return TbsGatt.CALL_CONTROL_POINT_RESULT_INVALID_OUTGOING_URI;"[m
[31m-        errorLine2="                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="763"[m
[31m-            column="32"/>[m
[32m+[m[32m            line="763"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                return TbsGatt.CALL_CONTROL_POINT_RESULT_OPERATION_NOT_POSSIBLE;"[m
[31m-        errorLine2="                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="770"[m
[31m-            column="32"/>[m
[32m+[m[32m            line="770"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        return TbsGatt.CALL_CONTROL_POINT_RESULT_SUCCESS;"[m
[31m-        errorLine2="                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="778"[m
[31m-            column="24"/>[m
[32m+[m[32m            line="778"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    case TbsGatt.CALL_CONTROL_POINT_OPCODE_ACCEPT:"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="818"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="818"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    case TbsGatt.CALL_CONTROL_POINT_OPCODE_TERMINATE:"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="819"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="819"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    case TbsGatt.CALL_CONTROL_POINT_OPCODE_LOCAL_HOLD:"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="820"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="820"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    case TbsGatt.CALL_CONTROL_POINT_OPCODE_LOCAL_RETRIEVE: {"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="821"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="821"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                            result = TbsGatt.CALL_CONTROL_POINT_RESULT_OPERATION_NOT_POSSIBLE;"[m
[31m-        errorLine2="                                             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="823"[m
[31m-            column="46"/>[m
[32m+[m[32m            line="823"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                            result = TbsGatt.CALL_CONTROL_POINT_RESULT_INVALID_CALL_INDEX;"[m
[31m-        errorLine2="                                             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="830"[m
[31m-            column="46"/>[m
[32m+[m[32m            line="830"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                            result = TbsGatt.CALL_CONTROL_POINT_RESULT_STATE_MISMATCH;"[m
[31m-        errorLine2="                                             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="836"[m
[31m-            column="46"/>[m
[32m+[m[32m            line="836"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                            if (opcode == TbsGatt.CALL_CONTROL_POINT_OPCODE_ACCEPT) {"[m
[31m-        errorLine2="                                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="845"[m
[31m-            column="51"/>[m
[32m+[m[32m            line="845"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                            } else if (opcode == TbsGatt.CALL_CONTROL_POINT_OPCODE_TERMINATE) {"[m
[31m-        errorLine2="                                                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="848"[m
[31m-            column="58"/>[m
[32m+[m[32m            line="848"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                            } else if (opcode == TbsGatt.CALL_CONTROL_POINT_OPCODE_LOCAL_HOLD) {"[m
[31m-        errorLine2="                                                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="850"[m
[31m-            column="58"/>[m
[32m+[m[32m            line="850"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                                    result = TbsGatt.CALL_CONTROL_POINT_RESULT_OPCODE_NOT_SUPPORTED;"[m
[31m-        errorLine2="                                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="853"[m
[31m-            column="54"/>[m
[32m+[m[32m            line="853"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                                    result = TbsGatt.CALL_CONTROL_POINT_RESULT_OPCODE_NOT_SUPPORTED;"[m
[31m-        errorLine2="                                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="860"[m
[31m-            column="54"/>[m
[32m+[m[32m            line="860"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                            result = TbsGatt.CALL_CONTROL_POINT_RESULT_OPERATION_NOT_POSSIBLE;"[m
[31m-        errorLine2="                                             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="867"[m
[31m-            column="46"/>[m
[32m+[m[32m            line="867"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        result = TbsGatt.CALL_CONTROL_POINT_RESULT_SUCCESS;"[m
[31m-        errorLine2="                                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="874"[m
[31m-            column="42"/>[m
[32m+[m[32m            line="874"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    case TbsGatt.CALL_CONTROL_POINT_OPCODE_ORIGINATE: {"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="878"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="878"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                    case TbsGatt.CALL_CONTROL_POINT_OPCODE_JOIN: {"[m
[31m-        errorLine2="                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="883"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="883"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                            result = TbsGatt.CALL_CONTROL_POINT_RESULT_OPERATION_NOT_POSSIBLE;"[m
[31m-        errorLine2="                                             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="886"[m
[31m-            column="46"/>[m
[32m+[m[32m            line="886"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        result = TbsGatt.CALL_CONTROL_POINT_RESULT_SUCCESS;"[m
[31m-        errorLine2="                                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="892"[m
[31m-            column="42"/>[m
[32m+[m[32m            line="892"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                                result = TbsGatt.CALL_CONTROL_POINT_RESULT_INVALID_CALL_INDEX;"[m
[31m-        errorLine2="                                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="896"[m
[31m-            column="50"/>[m
[32m+[m[32m            line="896"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                                result = TbsGatt.CALL_CONTROL_POINT_RESULT_OPERATION_NOT_POSSIBLE;"[m
[31m-        errorLine2="                                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="909"[m
[31m-            column="50"/>[m
[32m+[m[32m            line="909"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        if (result != TbsGatt.CALL_CONTROL_POINT_RESULT_SUCCESS) {"[m
[31m-        errorLine2="                                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="916"[m
[31m-            column="47"/>[m
[32m+[m[32m            line="916"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                            result = TbsGatt.CALL_CONTROL_POINT_RESULT_OPERATION_NOT_POSSIBLE;"[m
[31m-        errorLine2="                                             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="927"[m
[31m-            column="46"/>[m
[32m+[m[32m            line="927"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        result = TbsGatt.CALL_CONTROL_POINT_RESULT_SUCCESS;"[m
[31m-        errorLine2="                                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="934"[m
[31m-            column="42"/>[m
[32m+[m[32m            line="934"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                        result = TbsGatt.CALL_CONTROL_POINT_RESULT_OPCODE_NOT_SUPPORTED;"[m
[31m-        errorLine2="                                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="939"[m
[31m-            column="42"/>[m
[32m+[m[32m            line="939"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="                if (result == TbsGatt.CALL_CONTROL_POINT_RESULT_SUCCESS) {"[m
[31m-        errorLine2="                                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="943"[m
[31m-            column="39"/>[m
[32m+[m[32m            line="943"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            case TbsGatt.CALL_CONTROL_POINT_OPCODE_ACCEPT:"[m
[31m-        errorLine2="                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="1133"[m
[31m-            column="26"/>[m
[32m+[m[32m            line="1133"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            case TbsGatt.CALL_CONTROL_POINT_OPCODE_TERMINATE:"[m
[31m-        errorLine2="                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="1139"[m
[31m-            column="26"/>[m
[32m+[m[32m            line="1139"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            case TbsGatt.CALL_CONTROL_POINT_OPCODE_LOCAL_HOLD:"[m
[31m-        errorLine2="                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="1143"[m
[31m-            column="26"/>[m
[32m+[m[32m            line="1143"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            case TbsGatt.CALL_CONTROL_POINT_OPCODE_LOCAL_RETRIEVE:"[m
[31m-        errorLine2="                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/tbs/TbsGeneric.java"[m
[31m-            line="1151"[m
[31m-            column="26"/>[m
[32m+[m[32m            line="1151"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        if (!mNativeInterface.sendATCmd(device,"[m
[31m-        errorLine2="                              ~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/VendorCommandResponseProcessor.java"[m
[31m-            line="119"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="119"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mDatabaseManager = Objects.requireNonNull(mAdapterService.getDatabase(),"[m
[31m-        errorLine2="                                                                  ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/vc/VolumeControlService.java"[m
[31m-            line="231"[m
[31m-            column="67"/>[m
[32m+[m[32m            line="231"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        ParcelUuid[] featureUuids = mAdapterService.getRemoteUuids(device);"[m
[31m-        errorLine2="                                                    ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/vc/VolumeControlService.java"[m
[31m-            line="375"[m
[31m-            column="53"/>[m
[32m+[m[32m            line="375"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        mDatabaseManager.setProfileConnectionPolicy(device, BluetoothProfile.VOLUME_CONTROL,"[m
[31m-        errorLine2="                         ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/vc/VolumeControlService.java"[m
[31m-            line="550"[m
[31m-            column="26"/>[m
[32m+[m[32m            line="550"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="        return mDatabaseManager.getProfileConnectionPolicy(device, BluetoothProfile.VOLUME_CONTROL);"[m
[31m-        errorLine2="                                ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/vc/VolumeControlService.java"[m
[31m-            line="564"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="564"/>[m
     </issue>[m
 [m
     <issue[m
         id="VisibleForTests"[m
[31m-        message="This method should only be accessed from tests or within private scope"[m
[31m-        errorLine1="            sm.sendMessage(VolumeControlStateMachine.STACK_EVENT, stackEvent);"[m
[31m-        errorLine2="                                                     ~~~~~~~~~~~">[m
[32m+[m[32m        message="This method should only be accessed from tests or within private scope">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/vc/VolumeControlService.java"[m
[31m-            line="987"[m
[31m-            column="54"/>[m
[31m-    </issue>[m
[31m-[m
[31m-    <issue[m
[31m-        id="ExtraText"[m
[31m-        message="Unexpected text found in manifest file: &quot;>&quot;"[m
[31m-        errorLine1=">"[m
[31m-        errorLine2="~">[m
[31m-        <location[m
[31m-            file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="510"[m
[31m-            column="1"/>[m
[32m+[m[32m            line="987"/>[m
     </issue>[m
 [m
     <issue[m
         id="ParcelClassLoader"[m
[31m-        message="Passing null here (to use the default class loader) will not work if you are restoring your own classes. Consider using for example `getClass().getClassLoader()` instead."[m
[31m-        errorLine1="                    return new HfpClientCall((BluetoothDevice) in.readParcelable(null),"[m
[31m-        errorLine2="                                                                  ~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Passing null here (to use the default class loader) will not work if you are restoring your own classes. Consider using for example `getClass().getClassLoader()` instead.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfpclient/connserv/HfpClientCall.java"[m
[31m-            line="275"[m
[31m-            column="67"/>[m
[32m+[m[32m            line="275"/>[m
     </issue>[m
 [m
     <issue[m
         id="MissingVersion"[m
[31m-        message="Should set `android:versionCode` to specify the application version"[m
[31m-        errorLine1="&lt;manifest xmlns:android=&quot;http://schemas.android.com/apk/res/android&quot;"[m
[31m-        errorLine2=" ~~~~~~~~">[m
[32m+[m[32m        message="Should set `android:versionCode` to specify the application version">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="2"[m
[31m-            column="2"/>[m
[32m+[m[32m            line="2"/>[m
     </issue>[m
 [m
     <issue[m
         id="MissingVersion"[m
[31m-        message="Should set `android:versionName` to specify the application version"[m
[31m-        errorLine1="&lt;manifest xmlns:android=&quot;http://schemas.android.com/apk/res/android&quot;"[m
[31m-        errorLine2=" ~~~~~~~~">[m
[32m+[m[32m        message="Should set `android:versionName` to specify the application version">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="2"[m
[31m-            column="2"/>[m
[32m+[m[32m            line="2"/>[m
     </issue>[m
 [m
     <issue[m
         id="SupportAnnotationUsage"[m
[31m-        message="Only specify one of `value`, `anyOf` or `allOf`"[m
[31m-        errorLine1="    @RequiresPermission("[m
[31m-        errorLine2="    ^">[m
[32m+[m[32m        message="Only specify one of `value`, `anyOf` or `allOf`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/AdapterService.java"[m
[31m-            line="551"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="551"/>[m
     </issue>[m
 [m
     <issue[m
         id="SupportAnnotationUsage"[m
[31m-        message="Only specify one of `value`, `anyOf` or `allOf`"[m
[31m-        errorLine1="        @RequiresPermission(value = android.Manifest.permission.BLUETOOTH_CONNECT,"[m
[31m-        errorLine2="        ^">[m
[32m+[m[32m        message="Only specify one of `value`, `anyOf` or `allOf`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/AdapterService.java"[m
[31m-            line="1958"[m
[31m-            column="9"/>[m
[32m+[m[32m            line="1958"/>[m
     </issue>[m
 [m
     <issue[m
         id="StringFormatInvalid"[m
[31m-        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% a`&apos;?"[m
[31m-        errorLine1="    &lt;string name=&quot;notification_sent_complete&quot; msgid=&quot;6293391081175517098&quot;>&quot;Zu 100 % abgeschlossen&quot;&lt;/string>"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% a`&apos;?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-de/strings.xml"[m
[31m-            line="47"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="StringFormatInvalid"[m
[31m-        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% b`&apos;?"[m
[31m-        errorLine1="    &lt;string name=&quot;notification_sent_complete&quot; msgid=&quot;6293391081175517098&quot;>&quot;100 % baigta&quot;&lt;/string>"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% b`&apos;?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-lt/strings.xml"[m
[31m-            line="47"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="StringFormatInvalid"[m
[31m-        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% c`&apos;?"[m
[31m-        errorLine1="    &lt;string name=&quot;notification_sent_complete&quot; msgid=&quot;6293391081175517098&quot;>&quot;100% complet&quot;&lt;/string>"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% c`&apos;?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ca/strings.xml"[m
[31m-            line="47"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="StringFormatInvalid"[m
[31m-        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% c`&apos;?"[m
[31m-        errorLine1="    &lt;string name=&quot;notification_sent_complete&quot; msgid=&quot;6293391081175517098&quot;>&quot;100% complete&quot;&lt;/string>"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% c`&apos;?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-en-rAU/strings.xml"[m
[31m-            line="47"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="StringFormatInvalid"[m
[31m-        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% c`&apos;?"[m
[31m-        errorLine1="    &lt;string name=&quot;notification_sent_complete&quot; msgid=&quot;6293391081175517098&quot;>&quot;100% complete&quot;&lt;/string>"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% c`&apos;?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-en-rCA/strings.xml"[m
[31m-            line="47"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="StringFormatInvalid"[m
[31m-        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% c`&apos;?"[m
[31m-        errorLine1="    &lt;string name=&quot;notification_sent_complete&quot; msgid=&quot;6293391081175517098&quot;>&quot;100% complete&quot;&lt;/string>"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% c`&apos;?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-en-rGB/strings.xml"[m
[31m-            line="47"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="StringFormatInvalid"[m
[31m-        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% c`&apos;?"[m
[31m-        errorLine1="    &lt;string name=&quot;notification_sent_complete&quot; msgid=&quot;6293391081175517098&quot;>&quot;100% complete&quot;&lt;/string>"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% c`&apos;?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-en-rIN/strings.xml"[m
[31m-            line="47"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="StringFormatInvalid"[m
[31m-        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% c`&apos;?"[m
[31m-        errorLine1="    &lt;string name=&quot;notification_sent_complete&quot; msgid=&quot;6293391081175517098&quot;>&quot;‎‏‎‎‎‎‎‏‎‏‏‏‎‎‎‎‎‏‎‎‎‎‎‏‏‏‎‏‏‏‏‏‏‏‎‏‎‏‏‏‎‏‎‏‎‏‏‎‏‎‎‏‏‏‎‏‏‏‎‏‏‎‎‎‎‎‏‏‎‎‏‎‎‎‎‏‎‏‎‎‎‎‎‎‎‏‏‏‏‎‏‎‏‎‏‎‎100% complete‎‏‎‎‏‎&quot;&lt;/string>"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% c`&apos;?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-en-rXC/strings.xml"[m
[31m-            line="47"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="StringFormatInvalid"[m
[31m-        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% c`&apos;?"[m
[31m-        errorLine1="    &lt;string name=&quot;notification_sent_complete&quot; msgid=&quot;6293391081175517098&quot;>&quot;100% completado&quot;&lt;/string>"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% c`&apos;?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-es/strings.xml"[m
[31m-            line="47"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="StringFormatInvalid"[m
[31m-        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% c`&apos;?"[m
[31m-        errorLine1="    &lt;string name=&quot;notification_sent_complete&quot; msgid=&quot;6293391081175517098&quot;>&quot;100% completado&quot;&lt;/string>"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% c`&apos;?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-gl/strings.xml"[m
[31m-            line="47"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="StringFormatInvalid"[m
[31m-        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% c`&apos;?"[m
[31m-        errorLine1="    &lt;string name=&quot;notification_sent_complete&quot; msgid=&quot;6293391081175517098&quot;>&quot;100% completato&quot;&lt;/string>"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% c`&apos;?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-it/strings.xml"[m
[31m-            line="47"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="StringFormatInvalid"[m
[31m-        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% c`&apos;?"[m
[31m-        errorLine1="    &lt;string name=&quot;notification_sent_complete&quot; msgid=&quot;6293391081175517098&quot;>&quot;100% concluído&quot;&lt;/string>"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% c`&apos;?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-pt-rPT/strings.xml"[m
[31m-            line="47"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="StringFormatInvalid"[m
[31m-        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% c`&apos;?"[m
[31m-        errorLine1="    &lt;string name=&quot;notification_sent_complete&quot; msgid=&quot;6293391081175517098&quot;>&quot;100% concluído&quot;&lt;/string>"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% c`&apos;?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-pt/strings.xml"[m
[31m-            line="47"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="StringFormatInvalid"[m
[31m-        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% e`&apos;?"[m
[31m-        errorLine1="    &lt;string name=&quot;notification_sent_complete&quot; msgid=&quot;6293391081175517098&quot;>&quot;100 % effectués&quot;&lt;/string>"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% e`&apos;?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-fr-rCA/strings.xml"[m
[31m-            line="47"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="StringFormatInvalid"[m
[31m-        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% e`&apos;?"[m
[31m-        errorLine1="    &lt;string name=&quot;notification_sent_complete&quot; msgid=&quot;6293391081175517098&quot;>&quot;100 % effectués&quot;&lt;/string>"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% e`&apos;?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-fr/strings.xml"[m
[31m-            line="47"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="StringFormatInvalid"[m
[31m-        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% f`&apos;?"[m
[31m-        errorLine1="    &lt;string name=&quot;notification_sent_complete&quot; msgid=&quot;6293391081175517098&quot;>&quot;100 % fuldført&quot;&lt;/string>"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% f`&apos;?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-da/strings.xml"[m
[31m-            line="47"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="StringFormatInvalid"[m
[31m-        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% f`&apos;?"[m
[31m-        errorLine1="    &lt;string name=&quot;notification_sent_complete&quot; msgid=&quot;6293391081175517098&quot;>&quot;100 % fullført&quot;&lt;/string>"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% f`&apos;?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-nb/strings.xml"[m
[31m-            line="47"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="StringFormatInvalid"[m
[31m-        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% f`&apos;?"[m
[31m-        errorLine1="    &lt;string name=&quot;notification_sent_complete&quot; msgid=&quot;6293391081175517098&quot;>&quot;100 % finalizat&quot;&lt;/string>"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% f`&apos;?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ro/strings.xml"[m
[31m-            line="47"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="StringFormatInvalid"[m
[31m-        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% h`&apos;?"[m
[31m-        errorLine1="    &lt;string name=&quot;notification_sent_complete&quot; msgid=&quot;6293391081175517098&quot;>&quot;100 % hotovo&quot;&lt;/string>"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% h`&apos;?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-sk/strings.xml"[m
[31m-            line="47"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="StringFormatInvalid"[m
[31m-        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% s`&apos;?"[m
[31m-        errorLine1="    &lt;string name=&quot;notification_sent_complete&quot; msgid=&quot;6293391081175517098&quot;>&quot;100% selesai&quot;&lt;/string>"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% s`&apos;?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-in/strings.xml"[m
[31m-            line="47"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="StringFormatInvalid"[m
[31m-        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% s`&apos;?"[m
[31m-        errorLine1="    &lt;string name=&quot;notification_sent_complete&quot; msgid=&quot;6293391081175517098&quot;>&quot;100 % slutfört&quot;&lt;/string>"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% s`&apos;?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-sv/strings.xml"[m
[31m-            line="47"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="StringFormatInvalid"[m
[31m-        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% c`&apos;?"[m
[31m-        errorLine1="    &lt;string name=&quot;notification_sent_complete&quot;>100% complete&lt;/string>"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Incorrect formatting string `notification_sent_complete`; missing conversion character in &apos;`% c`&apos;?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values/strings.xml"[m
[31m-            line="102"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="102"/>[m
     </issue>[m
 [m
     <issue[m
         id="StringFormatMatches"[m
[31m-        message="Inconsistent formatting types for argument #1 in format string `notification_sent_complete` (&apos;% d&apos;): Found both &apos;`d`&apos; here and &apos;`e`&apos; in values-eu/strings.xml"[m
[31m-        errorLine1="    &lt;string name=&quot;notification_sent_complete&quot; msgid=&quot;6293391081175517098&quot;>&quot;100% dovršeno&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                              ~~~">[m
[32m+[m[32m        message="Inconsistent formatting types for argument #1 in format string `notification_sent_complete` (&apos;% d&apos;): Found both &apos;`d`&apos; here and &apos;`e`&apos; in values-eu/strings.xml">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-hr/strings.xml"[m
[31m-            line="47"[m
[31m-            column="79"/>[m
[32m+[m[32m            line="47"/>[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-eu/strings.xml"[m
[31m-            line="47"[m
[31m-            column="5"[m
[31m-            message="Conflicting argument type (`e&apos;) here"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="StringFormatCount"[m
[31m-        message="Inconsistent number of arguments in formatting string `notification_sent_complete`; found both 1 here and 0 in values-zh-rTW/strings.xml"[m
[31m-        errorLine1="    &lt;string name=&quot;notification_sent_complete&quot;>100% complete&lt;/string>"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Inconsistent number of arguments in formatting string `notification_sent_complete`; found both 1 here and 0 in values-zh-rTW/strings.xml">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values/strings.xml"[m
[31m-            line="102"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="102"/>[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-zh-rTW/strings.xml"[m
[31m-            line="47"[m
[31m-            column="5"[m
[31m-            message="Conflicting number of arguments (0) here"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedContentProvider"[m
[31m-        message="Exported content providers can provide access to potentially sensitive data"[m
[31m-        errorLine1="        &lt;provider"[m
[31m-        errorLine2="         ~~~~~~~~">[m
[32m+[m[32m        message="Exported content providers can provide access to potentially sensitive data">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="189"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="189"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedService"[m
[31m-        message="Exported service does not require permission"[m
[31m-        errorLine1="        &lt;service"[m
[31m-        errorLine2="         ~~~~~~~">[m
[32m+[m[32m        message="Exported service does not require permission">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="121"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="121"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedService"[m
[31m-        message="Exported service does not require permission"[m
[31m-        errorLine1="        &lt;service"[m
[31m-        errorLine2="         ~~~~~~~">[m
[32m+[m[32m        message="Exported service does not require permission">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="132"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="132"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedService"[m
[31m-        message="Exported service does not require permission"[m
[31m-        errorLine1="        &lt;service"[m
[31m-        errorLine2="         ~~~~~~~">[m
[32m+[m[32m        message="Exported service does not require permission">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="143"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="143"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedService"[m
[31m-        message="Exported service does not require permission"[m
[31m-        errorLine1="        &lt;service"[m
[31m-        errorLine2="         ~~~~~~~">[m
[32m+[m[32m        message="Exported service does not require permission">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="154"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="154"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedService"[m
[31m-        message="Exported service does not require permission"[m
[31m-        errorLine1="        &lt;service"[m
[31m-        errorLine2="         ~~~~~~~">[m
[32m+[m[32m        message="Exported service does not require permission">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="165"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="165"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedService"[m
[31m-        message="Exported service does not require permission"[m
[31m-        errorLine1="        &lt;service"[m
[31m-        errorLine2="         ~~~~~~~">[m
[32m+[m[32m        message="Exported service does not require permission">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="174"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="174"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedService"[m
[31m-        message="Exported service does not require permission"[m
[31m-        errorLine1="        &lt;service"[m
[31m-        errorLine2="         ~~~~~~~">[m
[32m+[m[32m        message="Exported service does not require permission">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="208"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="208"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedService"[m
[31m-        message="Exported service does not require permission"[m
[31m-        errorLine1="        &lt;service"[m
[31m-        errorLine2="         ~~~~~~~">[m
[32m+[m[32m        message="Exported service does not require permission">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="219"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="219"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedService"[m
[31m-        message="Exported service does not require permission"[m
[31m-        errorLine1="        &lt;service"[m
[31m-        errorLine2="         ~~~~~~~">[m
[32m+[m[32m        message="Exported service does not require permission">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="230"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="230"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedService"[m
[31m-        message="Exported service does not require permission"[m
[31m-        errorLine1="        &lt;service"[m
[31m-        errorLine2="         ~~~~~~~">[m
[32m+[m[32m        message="Exported service does not require permission">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="241"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="241"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedService"[m
[31m-        message="Exported service does not require permission"[m
[31m-        errorLine1="        &lt;service"[m
[31m-        errorLine2="         ~~~~~~~">[m
[32m+[m[32m        message="Exported service does not require permission">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="264"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="264"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedService"[m
[31m-        message="Exported service does not require permission"[m
[31m-        errorLine1="        &lt;service"[m
[31m-        errorLine2="         ~~~~~~~">[m
[32m+[m[32m        message="Exported service does not require permission">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="275"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="275"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedService"[m
[31m-        message="Exported service does not require permission"[m
[31m-        errorLine1="        &lt;service"[m
[31m-        errorLine2="         ~~~~~~~">[m
[32m+[m[32m        message="Exported service does not require permission">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="300"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="300"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedService"[m
[31m-        message="Exported service does not require permission"[m
[31m-        errorLine1="        &lt;service"[m
[31m-        errorLine2="         ~~~~~~~">[m
[32m+[m[32m        message="Exported service does not require permission">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="321"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="321"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedService"[m
[31m-        message="Exported service does not require permission"[m
[31m-        errorLine1="        &lt;service"[m
[31m-        errorLine2="         ~~~~~~~">[m
[32m+[m[32m        message="Exported service does not require permission">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="332"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="332"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedService"[m
[31m-        message="Exported service does not require permission"[m
[31m-        errorLine1="        &lt;service"[m
[31m-        errorLine2="         ~~~~~~~">[m
[32m+[m[32m        message="Exported service does not require permission">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="343"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="343"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedService"[m
[31m-        message="Exported service does not require permission"[m
[31m-        errorLine1="        &lt;service"[m
[31m-        errorLine2="         ~~~~~~~">[m
[32m+[m[32m        message="Exported service does not require permission">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="373"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="373"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedService"[m
[31m-        message="Exported service does not require permission"[m
[31m-        errorLine1="        &lt;service"[m
[31m-        errorLine2="         ~~~~~~~">[m
[32m+[m[32m        message="Exported service does not require permission">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="384"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="384"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedService"[m
[31m-        message="Exported service does not require permission"[m
[31m-        errorLine1="        &lt;service"[m
[31m-        errorLine2="         ~~~~~~~">[m
[32m+[m[32m        message="Exported service does not require permission">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="550"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="549"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedService"[m
[31m-        message="Exported service does not require permission"[m
[31m-        errorLine1="        &lt;service"[m
[31m-        errorLine2="         ~~~~~~~">[m
[32m+[m[32m        message="Exported service does not require permission">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="581"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="580"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedService"[m
[31m-        message="Exported service does not require permission"[m
[31m-        errorLine1="        &lt;service"[m
[31m-        errorLine2="         ~~~~~~~">[m
[32m+[m[32m        message="Exported service does not require permission">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="590"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="589"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedService"[m
[31m-        message="Exported service does not require permission"[m
[31m-        errorLine1="        &lt;service"[m
[31m-        errorLine2="         ~~~~~~~">[m
[32m+[m[32m        message="Exported service does not require permission">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="605"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="604"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedService"[m
[31m-        message="Exported service does not require permission"[m
[31m-        errorLine1="        &lt;service"[m
[31m-        errorLine2="         ~~~~~~~">[m
[32m+[m[32m        message="Exported service does not require permission">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="616"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="615"/>[m
     </issue>[m
 [m
     <issue[m
         id="ExportedService"[m
[31m-        message="Exported service does not require permission"[m
[31m-        errorLine1="        &lt;service"[m
[31m-        errorLine2="         ~~~~~~~">[m
[32m+[m[32m        message="Exported service does not require permission">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="627"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="626"/>[m
     </issue>[m
 [m
     <issue[m
         id="InvalidPermission"[m
[31m-        message="Protecting an unsupported element with a permission is a no-op and potentially dangerous"[m
[31m-        errorLine1="                android:permission=&quot;android.permission.ACCESS_BLUETOOTH_SHARE&quot; />"[m
[31m-        errorLine2="                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Protecting an unsupported element with a permission is a no-op and potentially dangerous">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="410"[m
[31m-            column="17"/>[m
[32m+[m[32m            line="410"/>[m
     </issue>[m
 [m
     <issue[m
         id="UnspecifiedRegisterReceiverFlag"[m
[31m-        message="`mBluetoothConnectionReceiver` \&#xA;is missing `RECEIVER_EXPORTED` or `RECEIVER_NOT_EXPORTED` flag for unprotected \&#xA;broadcasts registered for an IntentFilter that cannot be inspected by lint"[m
[31m-        errorLine1="        mService.registerReceiver(mBluetoothConnectionReceiver,"[m
[31m-        errorLine2="        ^">[m
[32m+[m[32m        message="`mBluetoothConnectionReceiver` \&#xA;is missing `RECEIVER_EXPORTED` or `RECEIVER_NOT_EXPORTED` flag for unprotected \&#xA;broadcasts registered for an IntentFilter that cannot be inspected by lint">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/gatt/ScanManager.java"[m
[31m-            line="205"[m
[31m-            column="9"/>[m
[32m+[m[32m            line="205"/>[m
     </issue>[m
 [m
     <issue[m
         id="QueryAllPackagesPermission"[m
[31m-        message="A `&lt;queries>` declaration should generally be used instead of QUERY_ALL_PACKAGES; \&#xA;see https://g.co/dev/packagevisibility for details"[m
[31m-        errorLine1="    &lt;uses-permission android:name=&quot;android.permission.QUERY_ALL_PACKAGES&quot; />"[m
[31m-        errorLine2="                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="A `&lt;queries>` declaration should generally be used instead of QUERY_ALL_PACKAGES; \&#xA;see https://g.co/dev/packagevisibility for details">[m
         <location[m
             file="./out/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/e10894d3da5db41239526c9048962355/manifest_merger/AndroidManifest.xml"[m
[31m-            line="61"[m
[31m-            column="22"/>[m
[32m+[m[32m            line="61"/>[m
     </issue>[m
 [m
     <issue[m
         id="WakelockTimeout"[m
[31m-        message="Provide a timeout when requesting a wakelock with `PowerManager.Wakelock.acquire(long timeout)`. This will ensure the OS will cleanup any wakelocks that last longer than you intend, and will save your user&apos;s battery."[m
[31m-        errorLine1="                mWakeLock.acquire();"[m
[31m-        errorLine2="                ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Provide a timeout when requesting a wakelock with `PowerManager.Wakelock.acquire(long timeout)`. This will ensure the OS will cleanup any wakelocks that last longer than you intend, and will save your user&apos;s battery.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/AdapterService.java"[m
[31m-            line="6312"[m
[31m-            column="17"/>[m
[32m+[m[32m            line="6312"/>[m
     </issue>[m
 [m
     <issue[m
         id="WakelockTimeout"[m
[31m-        message="Provide a timeout when requesting a wakelock with `PowerManager.Wakelock.acquire(long timeout)`. This will ensure the OS will cleanup any wakelocks that last longer than you intend, and will save your user&apos;s battery."[m
[31m-        errorLine1="            mWakeLock.acquire();"[m
[31m-        errorLine2="            ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Provide a timeout when requesting a wakelock with `PowerManager.Wakelock.acquire(long timeout)`. This will ensure the OS will cleanup any wakelocks that last longer than you intend, and will save your user&apos;s battery.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapService.java"[m
[31m-            line="256"[m
[31m-            column="13"/>[m
[32m+[m[32m            line="256"/>[m
     </issue>[m
 [m
     <issue[m
         id="WakelockTimeout"[m
[31m-        message="Provide a timeout when requesting a wakelock with `PowerManager.Wakelock.acquire(long timeout)`. This will ensure the OS will cleanup any wakelocks that last longer than you intend, and will save your user&apos;s battery."[m
[31m-        errorLine1="                        mWakeLock.acquire();"[m
[31m-        errorLine2="                        ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Provide a timeout when requesting a wakelock with `PowerManager.Wakelock.acquire(long timeout)`. This will ensure the OS will cleanup any wakelocks that last longer than you intend, and will save your user&apos;s battery.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapService.java"[m
[31m-            line="428"[m
[31m-            column="25"/>[m
[32m+[m[32m            line="428"/>[m
     </issue>[m
 [m
     <issue[m
         id="WakelockTimeout"[m
[31m-        message="Provide a timeout when requesting a wakelock with `PowerManager.Wakelock.acquire(long timeout)`. This will ensure the OS will cleanup any wakelocks that last longer than you intend, and will save your user&apos;s battery."[m
[31m-        errorLine1="            mWakeLock.acquire();"[m
[31m-        errorLine2="            ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Provide a timeout when requesting a wakelock with `PowerManager.Wakelock.acquire(long timeout)`. This will ensure the OS will cleanup any wakelocks that last longer than you intend, and will save your user&apos;s battery.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/opp/BluetoothOppObexClientSession.java"[m
[31m-            line="195"[m
[31m-            column="13"/>[m
[32m+[m[32m            line="195"/>[m
     </issue>[m
 [m
     <issue[m
         id="WakelockTimeout"[m
[31m-        message="Provide a timeout when requesting a wakelock with `PowerManager.Wakelock.acquire(long timeout)`. This will ensure the OS will cleanup any wakelocks that last longer than you intend, and will save your user&apos;s battery."[m
[31m-        errorLine1="            mPartialWakeLock.acquire();"[m
[31m-        errorLine2="            ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Provide a timeout when requesting a wakelock with `PowerManager.Wakelock.acquire(long timeout)`. This will ensure the OS will cleanup any wakelocks that last longer than you intend, and will save your user&apos;s battery.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/opp/BluetoothOppObexServerSession.java"[m
[31m-            line="314"[m
[31m-            column="13"/>[m
[32m+[m[32m            line="314"/>[m
     </issue>[m
 [m
     <issue[m
         id="WakelockTimeout"[m
[31m-        message="Provide a timeout when requesting a wakelock with `PowerManager.Wakelock.acquire(long timeout)`. This will ensure the OS will cleanup any wakelocks that last longer than you intend, and will save your user&apos;s battery."[m
[31m-        errorLine1="                        mWakeLock.acquire();"[m
[31m-        errorLine2="                        ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Provide a timeout when requesting a wakelock with `PowerManager.Wakelock.acquire(long timeout)`. This will ensure the OS will cleanup any wakelocks that last longer than you intend, and will save your user&apos;s battery.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/pbap/BluetoothPbapService.java"[m
[31m-            line="478"[m
[31m-            column="25"/>[m
[32m+[m[32m            line="478"/>[m
     </issue>[m
 [m
     <issue[m
         id="WakelockTimeout"[m
[31m-        message="Provide a timeout when requesting a wakelock with `PowerManager.Wakelock.acquire(long timeout)`. This will ensure the OS will cleanup any wakelocks that last longer than you intend, and will save your user&apos;s battery."[m
[31m-        errorLine1="            mWakeLock.acquire();"[m
[31m-        errorLine2="            ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Provide a timeout when requesting a wakelock with `PowerManager.Wakelock.acquire(long timeout)`. This will ensure the OS will cleanup any wakelocks that last longer than you intend, and will save your user&apos;s battery.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/sap/SapService.java"[m
[31m-            line="290"[m
[31m-            column="13"/>[m
[32m+[m[32m            line="290"/>[m
     </issue>[m
 [m
     <issue[m
         id="WakelockTimeout"[m
[31m-        message="Provide a timeout when requesting a wakelock with `PowerManager.Wakelock.acquire(long timeout)`. This will ensure the OS will cleanup any wakelocks that last longer than you intend, and will save your user&apos;s battery."[m
[31m-        errorLine1="                        mWakeLock.acquire();"[m
[31m-        errorLine2="                        ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Provide a timeout when requesting a wakelock with `PowerManager.Wakelock.acquire(long timeout)`. This will ensure the OS will cleanup any wakelocks that last longer than you intend, and will save your user&apos;s battery.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/sap/SapService.java"[m
[31m-            line="490"[m
[31m-            column="25"/>[m
[31m-    </issue>[m
[31m-[m
[31m-    <issue[m
[31m-        id="Recycle"[m
[31m-        message="This `ContentProviderClient` should be freed up after use with `#release()`"[m
[31m-        errorLine1="        if (contentResolver.acquireContentProviderClient(BluetoothShare.CONTENT_URI) == null) {"[m
[31m-        errorLine2="                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[31m-        <location[m
[31m-            file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/opp/BluetoothOppService.java"[m
[31m-            line="1116"[m
[31m-            column="29"/>[m
[32m+[m[32m            line="490"/>[m
     </issue>[m
 [m
     <issue[m
         id="Recycle"[m
[31m-        message="This `Cursor` should be freed up after use with `#close()`"[m
[31m-        errorLine1="            Cursor cursor = mResolver.query(uri, null, null, null, null);"[m
[31m-        errorLine2="                                      ~~~~~">[m
[32m+[m[32m        message="This `Cursor` should be freed up after use with `#close()`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/mapclient/MapClientContent.java"[m
[31m-            line="283"[m
[31m-            column="39"/>[m
[32m+[m[32m            line="283"/>[m
     </issue>[m
 [m
     <issue[m
         id="Recycle"[m
[31m-        message="This `Cursor` should be freed up after use with `#close()`"[m
[31m-        errorLine1="        Cursor threadCursor = resolver.query(uri, null, null, null, null);"[m
[31m-        errorLine2="                                       ~~~~~">[m
[32m+[m[32m        message="This `Cursor` should be freed up after use with `#close()`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/mapclient/MapClientContent.java"[m
[31m-            line="436"[m
[31m-            column="40"/>[m
[32m+[m[32m            line="436"/>[m
     </issue>[m
 [m
     <issue[m
         id="Recycle"[m
[31m-        message="This `Cursor` should be freed up after use with `#close()`"[m
[31m-        errorLine1="                .query(Uri.withAppendedPath(MmsSms.CONTENT_CONVERSATIONS_URI,"[m
[31m-        errorLine2="                 ~~~~~">[m
[32m+[m[32m        message="This `Cursor` should be freed up after use with `#close()`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/mapclient/MapClientContent.java"[m
[31m-            line="528"[m
[31m-            column="18"/>[m
[32m+[m[32m            line="528"/>[m
     </issue>[m
 [m
     <issue[m
         id="Recycle"[m
[31m-        message="This `Cursor` should be freed up after use with `#close()`"[m
[31m-        errorLine1="                    .query(Uri.parse(&quot;content://mms-sms/canonical-address/&quot; + recipient), null,"[m
[31m-        errorLine2="                     ~~~~~">[m
[32m+[m[32m        message="This `Cursor` should be freed up after use with `#close()`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/mapclient/MapClientContent.java"[m
[31m-            line="550"[m
[31m-            column="22"/>[m
[32m+[m[32m            line="550"/>[m
     </issue>[m
 [m
     <issue[m
         id="ObsoleteSdkInt"[m
[31m-        message="Unnecessary; SDK_INT is always >= 19"[m
[31m-        errorLine1="@TargetApi(19)"[m
[31m-        errorLine2="~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Unnecessary; SDK_INT is always >= 19">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapContent.java"[m
[31m-            line="63"[m
[31m-            column="1"/>[m
[32m+[m[32m            line="63"/>[m
     </issue>[m
 [m
     <issue[m
         id="ObsoleteSdkInt"[m
[31m-        message="Unnecessary; SDK_INT is always >= 19"[m
[31m-        errorLine1="@TargetApi(19)"[m
[31m-        errorLine2="~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Unnecessary; SDK_INT is always >= 19">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/BluetoothMapContentObserver.java"[m
[31m-            line="85"[m
[31m-            column="1"/>[m
[32m+[m[32m            line="85"/>[m
     </issue>[m
 [m
     <issue[m
         id="ObsoleteSdkInt"[m
[31m-        message="Unnecessary; SDK_INT is always >= 19"[m
[31m-        errorLine1="@TargetApi(19)"[m
[31m-        errorLine2="~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Unnecessary; SDK_INT is always >= 19">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/MmsFileProvider.java"[m
[31m-            line="40"[m
[31m-            column="1"/>[m
[32m+[m[32m            line="40"/>[m
     </issue>[m
 [m
     <issue[m
         id="ObsoleteSdkInt"[m
[31m-        message="Unnecessary; SDK_INT is always >= 5"[m
[31m-        errorLine1="@TargetApi(Build.VERSION_CODES.ECLAIR)"[m
[31m-        errorLine2="~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Unnecessary; SDK_INT is always >= 5">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/sap/SapService.java"[m
[31m-            line="46"[m
[31m-            column="1"/>[m
[32m+[m[32m            line="46"/>[m
     </issue>[m
 [m
     <issue[m
         id="ObsoleteSdkInt"[m
[31m-        message="Unnecessary; SDK_INT is always >= 19"[m
[31m-        errorLine1="@TargetApi(19)"[m
[31m-        errorLine2="~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Unnecessary; SDK_INT is always >= 19">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/map/SmsMmsContacts.java"[m
[31m-            line="40"[m
[31m-            column="1"/>[m
[32m+[m[32m            line="40"/>[m
     </issue>[m
 [m
     <issue[m
         id="ObsoleteSdkInt"[m
[31m-        message="Unnecessary; SDK_INT is always >= 33"[m
[31m-        errorLine1="    @RequiresApi(Build.VERSION_CODES.TIRAMISU)"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Unnecessary; SDK_INT is always >= 33">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/Utils.java"[m
[31m-            line="426"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="426"/>[m
     </issue>[m
 [m
     <issue[m
         id="StaticFieldLeak"[m
[31m-        message="This `AsyncTask` class should be static or leaks might occur (anonymous android.os.AsyncTask)"[m
[31m-        errorLine1="        new AsyncTask&lt;Void, Void, Void>() {"[m
[31m-        errorLine2="        ^">[m
[32m+[m[32m        message="This `AsyncTask` class should be static or leaks might occur (anonymous android.os.AsyncTask)">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/AdapterService.java"[m
[31m-            line="659"[m
[31m-            column="9"/>[m
[32m+[m[32m            line="659"/>[m
     </issue>[m
 [m
     <issue[m
         id="StaticFieldLeak"[m
[31m-        message="Do not place Android context classes in static fields (static reference to `BluetoothOppManager` which has field `mContext` pointing to `Context`); this is a memory leak"[m
[31m-        errorLine1="    static BluetoothOppManager sInstance;"[m
[31m-        errorLine2="    ~~~~~~">[m
[32m+[m[32m        message="Do not place Android context classes in static fields (static reference to `BluetoothOppManager` which has field `mContext` pointing to `Context`); this is a memory leak">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/opp/BluetoothOppManager.java"[m
[31m-            line="67"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="67"/>[m
     </issue>[m
 [m
     <issue[m
         id="StaticFieldLeak"[m
[31m-        message="Do not place Android context classes in static fields (static reference to `BluetoothOppPreference` which has field `mContext` pointing to `Context`); this is a memory leak"[m
[31m-        errorLine1="    private static BluetoothOppPreference sInstance;"[m
[31m-        errorLine2="            ~~~~~~">[m
[32m+[m[32m        message="Do not place Android context classes in static fields (static reference to `BluetoothOppPreference` which has field `mContext` pointing to `Context`); this is a memory leak">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/opp/BluetoothOppPreference.java"[m
[31m-            line="51"[m
[31m-            column="13"/>[m
[32m+[m[32m            line="51"/>[m
     </issue>[m
 [m
     <issue[m
         id="StaticFieldLeak"[m
[31m-        message="Do not place Android context classes in static fields (static reference to `MediaControlProfile` which has field `mContext` pointing to `Context`); this is a memory leak"[m
[31m-        errorLine1="    private static MediaControlProfile sGmcsForTesting;"[m
[31m-        errorLine2="            ~~~~~~">[m
[32m+[m[32m        message="Do not place Android context classes in static fields (static reference to `MediaControlProfile` which has field `mContext` pointing to `Context`); this is a memory leak">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/mcp/McpService.java"[m
[31m-            line="48"[m
[31m-            column="13"/>[m
[32m+[m[32m            line="48"/>[m
     </issue>[m
 [m
     <issue[m
         id="StaticFieldLeak"[m
[31m-        message="Do not place Android context classes in static fields (static reference to `MediaPlayerList` which has field `mContext` pointing to `Context`); this is a memory leak"[m
[31m-        errorLine1="    static MediaPlayerList sMediaPlayerListForTesting = null;"[m
[31m-        errorLine2="    ~~~~~~">[m
[32m+[m[32m        message="Do not place Android context classes in static fields (static reference to `MediaPlayerList` which has field `mContext` pointing to `Context`); this is a memory leak">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/mcp/MediaControlProfile.java"[m
[31m-            line="79"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="StaticFieldLeak"[m
[31m-        message="Do not place Android context classes in static fields (static reference to `MediaPlayerWrapper` which has field `mContext` pointing to `Context`); this is a memory leak"[m
[31m-        errorLine1="    private static MediaPlayerWrapper sInjectedWrapper;"[m
[31m-        errorLine2="            ~~~~~~">[m
[32m+[m[32m        message="Do not place Android context classes in static fields (static reference to `MediaPlayerWrapper` which has field `mContext` pointing to `Context`); this is a memory leak">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/audio_util/mockable/MediaPlayerWrapperFactory.java"[m
[31m-            line="31"[m
[31m-            column="13"/>[m
[32m+[m[32m            line="31"/>[m
     </issue>[m
 [m
     <issue[m
         id="StaticFieldLeak"[m
[31m-        message="Do not place Android context classes in static fields (static reference to `MetricsLogger` which has field `mContext` pointing to `Context`); this is a memory leak"[m
[31m-        errorLine1="    private static volatile MetricsLogger sInstance = null;"[m
[31m-        errorLine2="            ~~~~~~">[m
[32m+[m[32m        message="Do not place Android context classes in static fields (static reference to `MetricsLogger` which has field `mContext` pointing to `Context`); this is a memory leak">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/MetricsLogger.java"[m
[31m-            line="65"[m
[31m-            column="13"/>[m
[32m+[m[32m            line="65"/>[m
     </issue>[m
 [m
     <issue[m
         id="HandlerLeak"[m
[31m-        message="This `Handler` class should be static or leaks might occur (com.android.bluetooth.btservice.AdapterService.AdapterServiceHandler)"[m
[31m-        errorLine1="    class AdapterServiceHandler extends Handler {"[m
[31m-        errorLine2="          ~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This `Handler` class should be static or leaks might occur (com.android.bluetooth.btservice.AdapterService.AdapterServiceHandler)">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/btservice/AdapterService.java"[m
[31m-            line="415"[m
[31m-            column="11"/>[m
[32m+[m[32m            line="415"/>[m
     </issue>[m
 [m
     <issue[m
         id="HandlerLeak"[m
[31m-        message="This `Handler` class should be static or leaks might occur (anonymous android.os.Handler)"[m
[31m-        errorLine1="    final Handler mTimeoutHandler = new Handler() {"[m
[31m-        errorLine2="                                    ^">[m
[32m+[m[32m        message="This `Handler` class should be static or leaks might occur (anonymous android.os.Handler)">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/opp/BluetoothOppBtEnablingActivity.java"[m
[31m-            line="127"[m
[31m-            column="37"/>[m
[32m+[m[32m            line="127"/>[m
     </issue>[m
 [m
     <issue[m
         id="HandlerLeak"[m
[31m-        message="This `Handler` class should be static or leaks might occur (anonymous android.os.Handler)"[m
[31m-        errorLine1="    private final Handler mTimeoutHandler = new Handler() {"[m
[31m-        errorLine2="                                            ^">[m
[32m+[m[32m        message="This `Handler` class should be static or leaks might occur (anonymous android.os.Handler)">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/opp/BluetoothOppIncomingFileConfirmActivity.java"[m
[31m-            line="235"[m
[31m-            column="45"/>[m
[32m+[m[32m            line="235"/>[m
     </issue>[m
 [m
     <issue[m
         id="HandlerLeak"[m
[31m-        message="This `Handler` class should be static or leaks might occur (anonymous android.os.Handler)"[m
[31m-        errorLine1="    private Handler mHandler = new Handler() {"[m
[31m-        errorLine2="                               ^">[m
[32m+[m[32m        message="This `Handler` class should be static or leaks might occur (anonymous android.os.Handler)">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/opp/BluetoothOppNotification.java"[m
[31m-            line="197"[m
[31m-            column="32"/>[m
[32m+[m[32m            line="197"/>[m
     </issue>[m
 [m
     <issue[m
         id="HandlerLeak"[m
[31m-        message="This `Handler` class should be static or leaks might occur (anonymous android.os.Handler)"[m
[31m-        errorLine1="    private Handler mHandler = new Handler() {"[m
[31m-        errorLine2="                               ^">[m
[32m+[m[32m        message="This `Handler` class should be static or leaks might occur (anonymous android.os.Handler)">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/opp/BluetoothOppService.java"[m
[31m-            line="358"[m
[31m-            column="32"/>[m
[32m+[m[32m            line="366"/>[m
     </issue>[m
 [m
     <issue[m
         id="HandlerLeak"[m
[31m-        message="This `Handler` class should be static or leaks might occur (anonymous android.os.Handler)"[m
[31m-        errorLine1="    private final Handler mTimeoutHandler = new Handler() {"[m
[31m-        errorLine2="                                            ^">[m
[32m+[m[32m        message="This `Handler` class should be static or leaks might occur (anonymous android.os.Handler)">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/pbap/BluetoothPbapActivity.java"[m
[31m-            line="269"[m
[31m-            column="45"/>[m
[32m+[m[32m            line="269"/>[m
     </issue>[m
 [m
     <issue[m
         id="HandlerLeak"[m
[31m-        message="This `Handler` class should be static or leaks might occur (com.android.bluetooth.hid.HidDeviceService.HidDeviceServiceHandler)"[m
[31m-        errorLine1="    private class HidDeviceServiceHandler extends Handler {"[m
[31m-        errorLine2="                  ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="This `Handler` class should be static or leaks might occur (com.android.bluetooth.hid.HidDeviceService.HidDeviceServiceHandler)">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hid/HidDeviceService.java"[m
[31m-            line="93"[m
[31m-            column="19"/>[m
[32m+[m[32m            line="93"/>[m
     </issue>[m
 [m
     <issue[m
         id="HandlerLeak"[m
[31m-        message="This `Handler` class should be static or leaks might occur (anonymous android.os.Handler)"[m
[31m-        errorLine1="    private final Handler mHandler = new Handler() {"[m
[31m-        errorLine2="                                     ^">[m
[32m+[m[32m        message="This `Handler` class should be static or leaks might occur (anonymous android.os.Handler)">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hid/HidHostService.java"[m
[31m-            line="174"[m
[31m-            column="38"/>[m
[32m+[m[32m            line="174"/>[m
     </issue>[m
 [m
     <issue[m
         id="HandlerLeak"[m
[31m-        message="This `Handler` class should be static or leaks might occur (anonymous android.os.Handler)"[m
[31m-        errorLine1="    private final Handler mHandler = new Handler() {"[m
[31m-        errorLine2="                                     ^">[m
[32m+[m[32m        message="This `Handler` class should be static or leaks might occur (anonymous android.os.Handler)">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/pan/PanService.java"[m
[31m-            line="220"[m
[31m-            column="38"/>[m
[32m+[m[32m            line="220"/>[m
     </issue>[m
 [m
     <issue[m
         id="HandlerLeak"[m
[31m-        message="This `Handler` class should be static or leaks might occur (anonymous android.os.Handler)"[m
[31m-        errorLine1="    private final Handler mSessionStatusHandler = new Handler() {"[m
[31m-        errorLine2="                                                  ^">[m
[32m+[m[32m        message="This `Handler` class should be static or leaks might occur (anonymous android.os.Handler)">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/sap/SapService.java"[m
[31m-            line="450"[m
[31m-            column="51"/>[m
[32m+[m[32m            line="450"/>[m
     </issue>[m
 [m
     <issue[m
         id="HandlerLeak"[m
[31m-        message="This `Handler` class should be static or leaks might occur (anonymous android.os.Handler)"[m
[31m-        errorLine1="    private final Handler mHandler = new Handler() {"[m
[31m-        errorLine2="                                     ^">[m
[32m+[m[32m        message="This `Handler` class should be static or leaks might occur (anonymous android.os.Handler)">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/sdp/SdpManager.java"[m
[31m-            line="506"[m
[31m-            column="38"/>[m
[32m+[m[32m            line="506"/>[m
     </issue>[m
 [m
     <issue[m
         id="UseSparseArrays"[m
[31m-        message="Use `new SparseIntArray(...)` instead for better performance"[m
[31m-        errorLine1="    private SparseArray&lt;Integer> mSettings = new SparseArray&lt;>();"[m
[31m-        errorLine2="                                             ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Use `new SparseIntArray(...)` instead for better performance">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/avrcpcontroller/PlayerApplicationSettings.java"[m
[31m-            line="70"[m
[31m-            column="46"/>[m
[32m+[m[32m            line="70"/>[m
     </issue>[m
 [m
     <issue[m
         id="UseValueOf"[m
[31m-        message="Use `Integer.valueOf(arg)` instead"[m
[31m-        errorLine1="                out.add(new Integer(arg));"[m
[31m-        errorLine2="                        ~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Use `Integer.valueOf(arg)` instead">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hfp/HeadsetStateMachine.java"[m
[31m-            line="1914"[m
[31m-            column="25"/>[m
[32m+[m[32m            line="1952"/>[m
     </issue>[m
 [m
     <issue[m
         id="UseValueOf"[m
[31m-        message="Use `Integer.valueOf(bufferSize)` instead"[m
[31m-        errorLine1="        msg.obj = bufferSize > 0 ? new Integer(bufferSize) : null;"[m
[31m-        errorLine2="                                   ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Use `Integer.valueOf(bufferSize)` instead">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/hid/HidDeviceService.java"[m
[31m-            line="900"[m
[31m-            column="36"/>[m
[32m+[m[32m            line="900"/>[m
     </issue>[m
 [m
     <issue[m
         id="UseValueOf"[m
[31m-        message="Use `Long.valueOf(Instant.now().toEpochMilli())` instead"[m
[31m-        errorLine1="                            timestamp = new Long(Instant.now().toEpochMilli());"[m
[31m-        errorLine2="                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Use `Long.valueOf(Instant.now().toEpochMilli())` instead">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/mapclient/MceStateMachine.java"[m
[31m-            line="758"[m
[31m-            column="41"/>[m
[32m+[m[32m            line="758"/>[m
     </issue>[m
 [m
     <issue[m
         id="UseValueOf"[m
[31m-        message="Use `Long.valueOf(millisecondsToMcsInterval(positionMs))` instead"[m
[31m-        errorLine1="                            ? new Long(millisecondsToMcsInterval(positionMs)).intValue()"[m
[31m-        errorLine2="                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Use `Long.valueOf(millisecondsToMcsInterval(positionMs))` instead">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/mcp/MediaControlGattService.java"[m
[31m-            line="620"[m
[31m-            column="31"/>[m
[32m+[m[32m            line="620"/>[m
     </issue>[m
 [m
     <issue[m
         id="UseValueOf"[m
[31m-        message="Use `Long.valueOf(mcsIntervalToMilliseconds(intVal))` instead"[m
[31m-        errorLine1="                intVal = new Long(mcsIntervalToMilliseconds(intVal)).intValue();"[m
[31m-        errorLine2="                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Use `Long.valueOf(mcsIntervalToMilliseconds(intVal))` instead">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/mcp/MediaControlGattService.java"[m
[31m-            line="1202"[m
[31m-            column="26"/>[m
[32m+[m[32m            line="1202"/>[m
     </issue>[m
 [m
     <issue[m
         id="UseValueOf"[m
[31m-        message="Use `Float.valueOf(64 * Math.log(speed) / Math.log(2))` instead"[m
[31m-        errorLine1="        return new Float(64 * Math.log(speed) / Math.log(2)).intValue();"[m
[31m-        errorLine2="               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Use `Float.valueOf(64 * Math.log(speed) / Math.log(2))` instead">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/mcp/MediaControlGattService.java"[m
[31m-            line="1772"[m
[31m-            column="16"/>[m
[32m+[m[32m            line="1772"/>[m
     </issue>[m
 [m
     <issue[m
         id="UseValueOf"[m
[31m-        message="Use `Float.valueOf(Math.pow(2, (speed.floatValue() / 64.0f)))` instead"[m
[31m-        errorLine1="        return new Float(Math.pow(2, (speed.floatValue() / 64.0f)));"[m
[31m-        errorLine2="               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Use `Float.valueOf(Math.pow(2, (speed.floatValue() / 64.0f)))` instead">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/mcp/MediaControlGattService.java"[m
[31m-            line="1776"[m
[31m-            column="16"/>[m
[32m+[m[32m            line="1776"/>[m
     </issue>[m
 [m
     <issue[m
         id="UseValueOf"[m
[31m-        message="Use `Long.valueOf(millisecondsToMcsInterval(positionMs))` instead"[m
[31m-        errorLine1="                ? new Long(millisecondsToMcsInterval(positionMs)).intValue()"[m
[31m-        errorLine2="                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Use `Long.valueOf(millisecondsToMcsInterval(positionMs))` instead">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/mcp/MediaControlGattService.java"[m
[31m-            line="1866"[m
[31m-            column="19"/>[m
[32m+[m[32m            line="1866"/>[m
     </issue>[m
 [m
     <issue[m
         id="UseValueOf"[m
[31m-        message="Use `Long.valueOf(millisecondsToMcsInterval(durationMs))` instead"[m
[31m-        errorLine1="                    ? new Long(millisecondsToMcsInterval(durationMs)).intValue()"[m
[31m-        errorLine2="                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Use `Long.valueOf(millisecondsToMcsInterval(durationMs))` instead">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/mcp/MediaControlGattService.java"[m
[31m-            line="1908"[m
[31m-            column="23"/>[m
[32m+[m[32m            line="1908"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;جارٍ استلام الملف...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ar/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Fayl qəbulu edilir...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-az/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Primanje datoteke...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-b+sr+Latn/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Атрыманне файла...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-be/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Файлът се получава...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-bg/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;S\&apos;està rebent un fitxer...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ca/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Přijímání souboru...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-cs/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Datei wird heruntergeladen...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-de/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Λήψη αρχείου...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-el/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Recibiendo archivo...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-es-rUS/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Faili vastuvõtmine ...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-et/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Recibindo ficheiro...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-gl/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;ફાઇલ પ્રાપ્ત થઈ રહી છે ...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-gu/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Primanje datoteke...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-hr/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Fájl fogadása...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-hu/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Ֆայլի ստացում...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-hy/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;ファイルを受信中...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ja/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;파일을 수신하는 중...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ko/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;ກຳລັງຮັບເອົາໄຟລ໌...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-lo/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Gaunamas failas...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-lt/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Notiek faila saņemšana...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-lv/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Примање датотеки...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-mk/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;फाइल प्राप्त करत आहे...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-mr/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Menerima fail...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ms/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Mottar fil ...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-nb/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;फाइल प्राप्त गर्दै...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ne/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;A receber ficheiro...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-pt-rPT/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Se primește fișierul...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ro/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Получение файла...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ru/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Prebieha prijímanie súboru...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-sk/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Prejemanje datoteke ...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-sl/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Примање датотеке...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-sr/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Tar emot fil ...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-sv/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Inapokea faili...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-sw/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;กำลังรับไฟล์...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-th/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Dosya alınıyor...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-tr/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Отримання файлу...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-uk/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;正在接收文件...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-zh-rCN/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;download_line5&quot; msgid=&quot;4124272066218470715&quot;>&quot;Yamukela ifayela...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                              ~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-zu/strings.xml"[m
[31m-            line="54"[m
[31m-            column="63"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;ፋይል በመላክ ላይ...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-am/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;جارٍ إرسال الملف...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ar/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Fayl göndərilir...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-az/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Slanje datoteke...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-b+sr+Latn/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Адпраўка файла...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-be/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Файлът се изпраща...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-bg/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;ফাইল পাঠানো হচ্ছে...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-bn/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;S\&apos;està enviant el fitxer...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ca/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Odesílání souboru...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-cs/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Datei wird gesendet...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-de/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Αποστολή αρχείου...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-el/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Enviando archivo...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-es-rUS/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Faili saatmine ...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-et/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Envoi de fichier en cours...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-fr/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Enviando ficheiro...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-gl/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Slanje datoteke...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-hr/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Fájl küldése...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-hu/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Ֆայլն ուղարկվում է...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-hy/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;ファイルを送信中...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ja/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;파일을 보내는 중...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ko/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;ກຳລັງສົ່ງໄຟລ໌...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-lo/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Siunčiamas failas...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-lt/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Notiek faila sūtīšana...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-lv/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Испраќање датотека...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-mk/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Menghantar fail...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ms/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Sender filen ...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-nb/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;फाइल पठाउँदै...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ne/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;A enviar ficheiro...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-pt-rPT/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Se trimite fișierul...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ro/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Отправка файла...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ru/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Odosielanie súboru...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-sk/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Pošiljanje datoteke ...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-sl/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Слање датотеке...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-sr/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Skickar fil ...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-sv/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Inatuma faili...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-sw/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;กำลังส่งไฟล์...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-th/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Nagpapadala ng file...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-tl/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Dosya gönderiliyor...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-tr/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Надсил-ня файлу...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-uk/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;正在发送文件...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-zh-rCN/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;upload_line5&quot; msgid=&quot;3477751464103201364&quot;>&quot;Ithumela ifayela...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                            ~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-zu/strings.xml"[m
[31m-            line="68"[m
[31m-            column="61"/>[m
[32m+[m[32m            line="68"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Lütfən, gözləyin...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-az/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Чакайце...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-be/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;দয়া করে অপেক্ষা করুন...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-bn/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Čekejte prosím...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-cs/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Vent...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-da/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Bitte warten...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-de/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Περιμένετε...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-el/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Por favor, espera...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-es-rUS/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Por favor, espera...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-es/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Palun oodake ...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-et/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Veuillez patienter...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-fr/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Agarda...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-gl/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;કૃપા કરીને રાહ જુઓ...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-gu/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;कृपया प्रतीक्षा करें...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-hi/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Kérjük, várjon...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-hu/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Խնդրում ենք սպասել...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-hy/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;יש להמתין...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-iw/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;お待ちください...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ja/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;გთხოვთ, მოითმინოთ...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ka/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;សូម​រង់ចាំ...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-km/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;ກະລຸນາລໍຖ້າ...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-lo/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Palaukite...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-lt/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Почекајте...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-mk/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Түр хүлээнэ үү...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-mn/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;कृपया प्रतीक्षा करा...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-mr/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Sila tunggu...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ms/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Vent litt ...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-nb/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;कृपया प्रतीक्षा गर्नुहोस्...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ne/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Een ogenblik geduld...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-nl/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;ਕਿਰਪਾ ਕਰਕੇ ਠਹਿਰੋ...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-pa/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Aguarde...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-pt-rPT/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Aguarde...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-pt/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Așteaptă...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ro/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Подождите...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ru/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;කරුණාකර රැඳී සිටින්න...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-si/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Čakajte...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-sk/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Počakajte ...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-sl/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Qëndro në pritje...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-sq/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;దయచేసి వేచి ఉండండి...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-te/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;โปรดรอสักครู่...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-th/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Lütfen bekleyin...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-tr/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Vui lòng đợi...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-vi/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;请稍候...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-zh-rCN/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_title&quot; msgid=&quot;5262637688863903594&quot;>&quot;Sicela ulinde...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                       ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-zu/strings.xml"[m
[31m-            line="79"[m
[31m-            column="72"/>[m
[32m+[m[32m            line="79"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;ብሉቱዝ በማብራት ላይ...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-am/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;جارٍ تفعيل البلوتوث...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ar/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;Bluetooth yandırılır...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-az/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;Уключэнне Bluetooth...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-be/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;Bluetooth се включва...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-bg/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;ব্লুটুথ চালু করা হচ্ছে...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-bn/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;S\&apos;està activant el Bluetooth...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ca/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;Aktiverer Bluetooth...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-da/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;Bluetooth wird aktiviert...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-de/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;Activando Bluetooth...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-es-rUS/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;Activando Bluetooth...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-es/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;Bluetoothi sisselülitamine ...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-et/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;Otetaan Bluetooth käyttöön...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-fi/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;Bluetooth bekapcsolása...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-hu/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;Bluetooth-ը միանում է...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-hy/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;Menghidupkan Bluetooth...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-in/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;BluetoothをONにしています...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ja/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;블루투스 켜는 중...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ko/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;ກຳລັງເປີດ Bluetooth...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-lo/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;Tiek ieslēgts Bluetooth savienojums...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-lv/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;Вклучување Bluetooth...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-mk/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;Menghidupkan Bluetooth...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ms/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;ब्लुटुथलाई सक्रिय पार्दै...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ne/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;Ativando Bluetooth...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-pt/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;Se activează Bluetooth...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ro/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;Включение Bluetooth...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-ru/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;Prebieha zapínanie rozhrania Bluetooth...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-sk/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;Inawasha Bluetooth...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-sw/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;Bluetooth açılıyor...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-tr/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;Увімкнення Bluetooth...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-uk/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;正在打开蓝牙...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-zh-rCN/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;正在開啟藍牙...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-zh-rHK/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;enabling_progress_content&quot; msgid=&quot;685427201206684584&quot;>&quot;Ivula i-Bluetooth...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                        ~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-zu/strings.xml"[m
[31m-            line="80"[m
[31m-            column="73"/>[m
[32m+[m[32m            line="80"/>[m
     </issue>[m
 [m
     <issue[m
         id="TypographyEllipsis"[m
[31m-        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?"[m
[31m-        errorLine1="    &lt;string name=&quot;bt_toast_4&quot; msgid=&quot;480365991944956695&quot;>&quot;Datei wird an „&lt;xliff:g id=&quot;RECIPIENT&quot;>%1$s&lt;/xliff:g>“ gesendet...&quot;&lt;/string>"[m
[31m-        errorLine2="                                                                                                 ~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Replace &quot;...&quot; with ellipsis character (…, &amp;#8230;) ?">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/values-de/strings.xml"[m
[31m-            line="84"[m
[31m-            column="98"/>[m
[32m+[m[32m            line="84"/>[m
     </issue>[m
 [m
     <issue[m
         id="MonochromeLauncherIcon"[m
[31m-        message="The application adaptive icon is missing a monochrome tag"[m
[31m-        errorLine1="&lt;adaptive-icon xmlns:android=&quot;http://schemas.android.com/apk/res/android&quot;>"[m
[31m-        errorLine2="^">[m
[32m+[m[32m        message="The application adaptive icon is missing a monochrome tag">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/mipmap-anydpi/bt_share.xml"[m
[31m-            line="16"[m
[31m-            column="1"/>[m
[32m+[m[32m            line="16"/>[m
     </issue>[m
 [m
     <issue[m
         id="TextFields"[m
[31m-        message="This text field does not specify an `inputType`"[m
[31m-        errorLine1="    &lt;EditText"[m
[31m-        errorLine2="     ~~~~~~~~">[m
[32m+[m[32m        message="This text field does not specify an `inputType`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/auth.xml"[m
[31m-            line="39"[m
[31m-            column="6"/>[m
[32m+[m[32m            line="39"/>[m
     </issue>[m
 [m
     <issue[m
         id="TextFields"[m
[31m-        message="This text field does not specify an `inputType`"[m
[31m-        errorLine1="        &lt;EditText android:id=&quot;@+id/insert_text&quot;"[m
[31m-        errorLine2="         ~~~~~~~~">[m
[32m+[m[32m        message="This text field does not specify an `inputType`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="14"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="14"/>[m
     </issue>[m
 [m
     <issue[m
         id="TextFields"[m
[31m-        message="This text field does not specify an `inputType`"[m
[31m-        errorLine1="        &lt;EditText android:id=&quot;@+id/media_text&quot;"[m
[31m-        errorLine2="         ~~~~~~~~">[m
[32m+[m[32m        message="This text field does not specify an `inputType`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="24"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="24"/>[m
     </issue>[m
 [m
     <issue[m
         id="TextFields"[m
[31m-        message="This text field does not specify an `inputType`"[m
[31m-        errorLine1="        &lt;EditText android:id=&quot;@+id/address_text&quot;"[m
[31m-        errorLine2="         ~~~~~~~~">[m
[32m+[m[32m        message="This text field does not specify an `inputType`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="28"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="28"/>[m
     </issue>[m
 [m
     <issue[m
         id="TextFields"[m
[31m-        message="This text field does not specify an `inputType`"[m
[31m-        errorLine1="        &lt;EditText android:id=&quot;@+id/delete_text&quot;"[m
[31m-        errorLine2="         ~~~~~~~~">[m
[32m+[m[32m        message="This text field does not specify an `inputType`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="47"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="TextFields"[m
[31m-        message="This text field does not specify an `inputType`"[m
[31m-        errorLine1="        &lt;EditText android:id=&quot;@+id/update_text&quot;"[m
[31m-        errorLine2="         ~~~~~~~~">[m
[32m+[m[32m        message="This text field does not specify an `inputType`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="65"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="65"/>[m
     </issue>[m
 [m
     <issue[m
         id="TextFields"[m
[31m-        message="This text field does not specify an `inputType`"[m
[31m-        errorLine1="        &lt;EditText android:id=&quot;@+id/ack_text&quot;"[m
[31m-        errorLine2="         ~~~~~~~~">[m
[32m+[m[32m        message="This text field does not specify an `inputType`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="83"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="83"/>[m
     </issue>[m
 [m
     <issue[m
         id="Autofill"[m
[31m-        message="Missing `autofillHints` attribute"[m
[31m-        errorLine1="    &lt;EditText"[m
[31m-        errorLine2="     ~~~~~~~~">[m
[32m+[m[32m        message="Missing `autofillHints` attribute">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/auth.xml"[m
[31m-            line="39"[m
[31m-            column="6"/>[m
[32m+[m[32m            line="39"/>[m
     </issue>[m
 [m
     <issue[m
         id="Autofill"[m
[31m-        message="Missing `autofillHints` attribute"[m
[31m-        errorLine1="        &lt;EditText android:id=&quot;@+id/insert_text&quot;"[m
[31m-        errorLine2="         ~~~~~~~~">[m
[32m+[m[32m        message="Missing `autofillHints` attribute">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="14"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="14"/>[m
     </issue>[m
 [m
     <issue[m
         id="Autofill"[m
[31m-        message="Missing `autofillHints` attribute"[m
[31m-        errorLine1="        &lt;EditText android:id=&quot;@+id/media_text&quot;"[m
[31m-        errorLine2="         ~~~~~~~~">[m
[32m+[m[32m        message="Missing `autofillHints` attribute">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="24"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="24"/>[m
     </issue>[m
 [m
     <issue[m
         id="Autofill"[m
[31m-        message="Missing `autofillHints` attribute"[m
[31m-        errorLine1="        &lt;EditText android:id=&quot;@+id/address_text&quot;"[m
[31m-        errorLine2="         ~~~~~~~~">[m
[32m+[m[32m        message="Missing `autofillHints` attribute">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="28"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="28"/>[m
     </issue>[m
 [m
     <issue[m
         id="Autofill"[m
[31m-        message="Missing `autofillHints` attribute"[m
[31m-        errorLine1="        &lt;EditText android:id=&quot;@+id/delete_text&quot;"[m
[31m-        errorLine2="         ~~~~~~~~">[m
[32m+[m[32m        message="Missing `autofillHints` attribute">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="47"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="Autofill"[m
[31m-        message="Missing `autofillHints` attribute"[m
[31m-        errorLine1="        &lt;EditText android:id=&quot;@+id/update_text&quot;"[m
[31m-        errorLine2="         ~~~~~~~~">[m
[32m+[m[32m        message="Missing `autofillHints` attribute">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="65"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="65"/>[m
     </issue>[m
 [m
     <issue[m
         id="Autofill"[m
[31m-        message="Missing `autofillHints` attribute"[m
[31m-        errorLine1="        &lt;EditText android:id=&quot;@+id/ack_text&quot;"[m
[31m-        errorLine2="         ~~~~~~~~">[m
[32m+[m[32m        message="Missing `autofillHints` attribute">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="83"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="83"/>[m
     </issue>[m
 [m
     <issue[m
         id="ContentDescription"[m
[31m-        message="Missing `contentDescription` attribute on image"[m
[31m-        errorLine1="    &lt;ImageView"[m
[31m-        errorLine2="     ~~~~~~~~~">[m
[32m+[m[32m        message="Missing `contentDescription` attribute on image">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/bluetooth_map_settings_account_group.xml"[m
[31m-            line="23"[m
[31m-            column="6"/>[m
[32m+[m[32m            line="23"/>[m
     </issue>[m
 [m
     <issue[m
         id="LabelFor"[m
[31m-        message="Missing accessibility label: provide either a view with an `android:labelFor` that references this view or provide an `android:hint`"[m
[31m-        errorLine1="    &lt;EditText"[m
[31m-        errorLine2="     ~~~~~~~~">[m
[32m+[m[32m        message="Missing accessibility label: provide either a view with an `android:labelFor` that references this view or provide an `android:hint`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/auth.xml"[m
[31m-            line="39"[m
[31m-            column="6"/>[m
[32m+[m[32m            line="39"/>[m
     </issue>[m
 [m
     <issue[m
         id="LabelFor"[m
[31m-        message="Missing accessibility label: provide either a view with an `android:labelFor` that references this view or provide an `android:hint`"[m
[31m-        errorLine1="        &lt;EditText android:id=&quot;@+id/insert_text&quot;"[m
[31m-        errorLine2="         ~~~~~~~~">[m
[32m+[m[32m        message="Missing accessibility label: provide either a view with an `android:labelFor` that references this view or provide an `android:hint`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="14"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="14"/>[m
     </issue>[m
 [m
     <issue[m
         id="LabelFor"[m
[31m-        message="Missing accessibility label: provide either a view with an `android:labelFor` that references this view or provide an `android:hint`"[m
[31m-        errorLine1="        &lt;EditText android:id=&quot;@+id/media_text&quot;"[m
[31m-        errorLine2="         ~~~~~~~~">[m
[32m+[m[32m        message="Missing accessibility label: provide either a view with an `android:labelFor` that references this view or provide an `android:hint`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="24"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="24"/>[m
     </issue>[m
 [m
     <issue[m
         id="LabelFor"[m
[31m-        message="Missing accessibility label: provide either a view with an `android:labelFor` that references this view or provide an `android:hint`"[m
[31m-        errorLine1="        &lt;EditText android:id=&quot;@+id/address_text&quot;"[m
[31m-        errorLine2="         ~~~~~~~~">[m
[32m+[m[32m        message="Missing accessibility label: provide either a view with an `android:labelFor` that references this view or provide an `android:hint`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="28"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="28"/>[m
     </issue>[m
 [m
     <issue[m
         id="LabelFor"[m
[31m-        message="Missing accessibility label: provide either a view with an `android:labelFor` that references this view or provide an `android:hint`"[m
[31m-        errorLine1="        &lt;EditText android:id=&quot;@+id/delete_text&quot;"[m
[31m-        errorLine2="         ~~~~~~~~">[m
[32m+[m[32m        message="Missing accessibility label: provide either a view with an `android:labelFor` that references this view or provide an `android:hint`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="47"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="47"/>[m
     </issue>[m
 [m
     <issue[m
         id="LabelFor"[m
[31m-        message="Missing accessibility label: provide either a view with an `android:labelFor` that references this view or provide an `android:hint`"[m
[31m-        errorLine1="        &lt;EditText android:id=&quot;@+id/update_text&quot;"[m
[31m-        errorLine2="         ~~~~~~~~">[m
[32m+[m[32m        message="Missing accessibility label: provide either a view with an `android:labelFor` that references this view or provide an `android:hint`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="65"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="65"/>[m
     </issue>[m
 [m
     <issue[m
         id="LabelFor"[m
[31m-        message="Missing accessibility label: provide either a view with an `android:labelFor` that references this view or provide an `android:hint`"[m
[31m-        errorLine1="        &lt;EditText android:id=&quot;@+id/ack_text&quot;"[m
[31m-        errorLine2="         ~~~~~~~~">[m
[32m+[m[32m        message="Missing accessibility label: provide either a view with an `android:labelFor` that references this view or provide an `android:hint`">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="83"[m
[31m-            column="10"/>[m
[32m+[m[32m            line="83"/>[m
     </issue>[m
 [m
     <issue[m
         id="SetTextI18n"[m
[31m-        message="Do not concatenate text displayed with `setText`. Use resource string with placeholders."[m
[31m-        errorLine1="                getString(R.string.bt_enable_line1) + &quot;\n\n&quot; + getString(R.string.bt_enable_line2)"[m
[31m-        errorLine2="                ^">[m
[32m+[m[32m        message="Do not concatenate text displayed with `setText`. Use resource string with placeholders.">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/src/com/android/bluetooth/opp/BluetoothOppBtEnableActivity.java"[m
[31m-            line="77"[m
[31m-            column="17"/>[m
[32m+[m[32m            line="77"/>[m
     </issue>[m
 [m
     <issue[m
         id="RelativeOverlap"[m
[31m-        message="`@id/bluetooth_map_settings_group_text_view` can overlap `@id/bluetooth_map_settings_group_checkbox` if @id/bluetooth_map_settings_group_text_view grows due to localized text expansion"[m
[31m-        errorLine1="    &lt;TextView"[m
[31m-        errorLine2="     ~~~~~~~~">[m
[32m+[m[32m        message="`@id/bluetooth_map_settings_group_text_view` can overlap `@id/bluetooth_map_settings_group_checkbox` if @id/bluetooth_map_settings_group_text_view grows due to localized text expansion">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/bluetooth_map_settings_account_group.xml"[m
[31m-            line="33"[m
[31m-            column="6"/>[m
[32m+[m[32m            line="33"/>[m
     </issue>[m
 [m
     <issue[m
         id="RtlSymmetry"[m
[31m-        message="When you define `paddingStart` you should probably also define `paddingEnd` for right-to-left symmetry"[m
[31m-        errorLine1="        android:paddingStart=&quot;10dip&quot;"[m
[31m-        errorLine2="        ~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="When you define `paddingStart` you should probably also define `paddingEnd` for right-to-left symmetry">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/bluetooth_transfer_item.xml"[m
[31m-            line="61"[m
[31m-            column="9"/>[m
[32m+[m[32m            line="61"/>[m
     </issue>[m
 [m
     <issue[m
         id="RtlHardcoded"[m
[31m-        message="Consider replacing `android:layout_alignParentLeft` with `android:layout_alignParentStart=&quot;true&quot;` to better support right-to-left layouts"[m
[31m-        errorLine1="        android:layout_alignParentLeft=&quot;true&quot;"[m
[31m-        errorLine2="        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Consider replacing `android:layout_alignParentLeft` with `android:layout_alignParentStart=&quot;true&quot;` to better support right-to-left layouts">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/bluetooth_map_settings_account_group.xml"[m
[31m-            line="27"[m
[31m-            column="9"/>[m
[32m+[m[32m            line="27"/>[m
     </issue>[m
 [m
     <issue[m
         id="RtlHardcoded"[m
[31m-        message="Consider replacing `android:layout_marginLeft` with `android:layout_marginStart=&quot;30dp&quot;` to better support right-to-left layouts"[m
[31m-        errorLine1="        android:layout_marginLeft=&quot;30dp&quot;"[m
[31m-        errorLine2="        ~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Consider replacing `android:layout_marginLeft` with `android:layout_marginStart=&quot;30dp&quot;` to better support right-to-left layouts">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/bluetooth_map_settings_account_group.xml"[m
[31m-            line="31"[m
[31m-            column="9"/>[m
[32m+[m[32m            line="31"/>[m
     </issue>[m
 [m
     <issue[m
         id="RtlHardcoded"[m
[31m-        message="Consider replacing `android:layout_toRightOf` with `android:layout_toEndOf=&quot;@id/bluetooth_map_settings_group_icon&quot;` to better support right-to-left layouts"[m
[31m-        errorLine1="        android:layout_toRightOf=&quot;@id/bluetooth_map_settings_group_icon&quot;/>"[m
[31m-        errorLine2="        ~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Consider replacing `android:layout_toRightOf` with `android:layout_toEndOf=&quot;@id/bluetooth_map_settings_group_icon&quot;` to better support right-to-left layouts">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/bluetooth_map_settings_account_group.xml"[m
[31m-            line="37"[m
[31m-            column="9"/>[m
[32m+[m[32m            line="37"/>[m
     </issue>[m
 [m
     <issue[m
         id="RtlHardcoded"[m
[31m-        message="Consider replacing `android:layout_alignParentRight` with `android:layout_alignParentEnd=&quot;true&quot;` to better support right-to-left layouts"[m
[31m-        errorLine1="        android:layout_alignParentRight=&quot;true&quot;"[m
[31m-        errorLine2="        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Consider replacing `android:layout_alignParentRight` with `android:layout_alignParentEnd=&quot;true&quot;` to better support right-to-left layouts">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/bluetooth_map_settings_account_group.xml"[m
[31m-            line="42"[m
[31m-            column="9"/>[m
[32m+[m[32m            line="42"/>[m
     </issue>[m
 [m
     <issue[m
         id="RtlHardcoded"[m
[31m-        message="Consider replacing `android:paddingLeft` with `android:paddingStart=&quot;51dp&quot;` to better support right-to-left layouts"[m
[31m-        errorLine1="    android:paddingLeft=&quot;51dp&quot;"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Consider replacing `android:paddingLeft` with `android:paddingStart=&quot;51dp&quot;` to better support right-to-left layouts">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/bluetooth_map_settings_account_item.xml"[m
[31m-            line="25"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="25"/>[m
     </issue>[m
 [m
     <issue[m
         id="RtlHardcoded"[m
[31m-        message="Consider replacing `android:paddingRight` with `android:paddingEnd=&quot;25dp&quot;` to better support right-to-left layouts"[m
[31m-        errorLine1="    android:paddingRight=&quot;25dp&quot;"[m
[31m-        errorLine2="    ~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Consider replacing `android:paddingRight` with `android:paddingEnd=&quot;25dp&quot;` to better support right-to-left layouts">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/bluetooth_map_settings_account_item.xml"[m
[31m-            line="26"[m
[31m-            column="5"/>[m
[32m+[m[32m            line="26"/>[m
     </issue>[m
 [m
     <issue[m
         id="RtlHardcoded"[m
[31m-        message="Use &quot;`start`&quot; instead of &quot;`left`&quot; to ensure correct behavior in right-to-left locales"[m
[31m-        errorLine1="            android:gravity=&quot;left|center_vertical&quot;/>"[m
[31m-        errorLine2="                             ~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Use &quot;`start`&quot; instead of &quot;`left`&quot; to ensure correct behavior in right-to-left locales">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/bluetooth_map_settings_account_item.xml"[m
[31m-            line="34"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="34"/>[m
     </issue>[m
 [m
     <issue[m
         id="RtlHardcoded"[m
[31m-        message="Use &quot;`end`&quot; instead of &quot;`right`&quot; to ensure correct behavior in right-to-left locales"[m
[31m-        errorLine1="            android:layout_gravity=&quot;right&quot;"[m
[31m-        errorLine2="                                    ~~~~~">[m
[32m+[m[32m        message="Use &quot;`end`&quot; instead of &quot;`right`&quot; to ensure correct behavior in right-to-left locales">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/bluetooth_map_settings_account_item.xml"[m
[31m-            line="40"[m
[31m-            column="37"/>[m
[32m+[m[32m            line="40"/>[m
     </issue>[m
 [m
     <issue[m
         id="RtlHardcoded"[m
[31m-        message="Use &quot;`end`&quot; instead of &quot;`right`&quot; to ensure correct behavior in right-to-left locales"[m
[31m-        errorLine1="            android:gravity=&quot;right|center_vertical&quot; />"[m
[31m-        errorLine2="                             ~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Use &quot;`end`&quot; instead of &quot;`right`&quot; to ensure correct behavior in right-to-left locales">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/bluetooth_map_settings_account_item.xml"[m
[31m-            line="42"[m
[31m-            column="30"/>[m
[32m+[m[32m            line="42"/>[m
     </issue>[m
 [m
     <issue[m
         id="RtlHardcoded"[m
[31m-        message="Consider replacing `android:layout_marginLeft` with `android:layout_marginStart=&quot;20dip&quot;` to better support right-to-left layouts"[m
[31m-        errorLine1="                android:layout_marginLeft=&quot;20dip&quot;"[m
[31m-        errorLine2="                ~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Consider replacing `android:layout_marginLeft` with `android:layout_marginStart=&quot;20dip&quot;` to better support right-to-left layouts">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/file_transfer.xml"[m
[31m-            line="86"[m
[31m-            column="17"/>[m
[32m+[m[32m            line="86"/>[m
     </issue>[m
 [m
     <issue[m
         id="RtlHardcoded"[m
[31m-        message="Use &quot;`start`&quot; instead of &quot;`left`&quot; to ensure correct behavior in right-to-left locales"[m
[31m-        errorLine1="                android:gravity=&quot;left&quot; />"[m
[31m-        errorLine2="                                 ~~~~">[m
[32m+[m[32m        message="Use &quot;`start`&quot; instead of &quot;`left`&quot; to ensure correct behavior in right-to-left locales">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/file_transfer.xml"[m
[31m-            line="88"[m
[31m-            column="34"/>[m
[32m+[m[32m            line="88"/>[m
     </issue>[m
 [m
     <issue[m
         id="RtlHardcoded"[m
[31m-        message="Consider replacing `android:layout_marginLeft` with `android:layout_marginStart=&quot;10dip&quot;` to better support right-to-left layouts"[m
[31m-        errorLine1="                android:layout_marginLeft=&quot;10dip&quot;"[m
[31m-        errorLine2="                ~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Consider replacing `android:layout_marginLeft` with `android:layout_marginStart=&quot;10dip&quot;` to better support right-to-left layouts">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/file_transfer.xml"[m
[31m-            line="93"[m
[31m-            column="17"/>[m
[32m+[m[32m            line="93"/>[m
     </issue>[m
 [m
     <issue[m
         id="RtlHardcoded"[m
[31m-        message="Consider replacing `android:layout_marginRight` with `android:layout_marginEnd=&quot;20dip&quot;` to better support right-to-left layouts"[m
[31m-        errorLine1="                android:layout_marginRight=&quot;20dip&quot;"[m
[31m-        errorLine2="                ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Consider replacing `android:layout_marginRight` with `android:layout_marginEnd=&quot;20dip&quot;` to better support right-to-left layouts">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/file_transfer.xml"[m
[31m-            line="94"[m
[31m-            column="17"/>[m
[32m+[m[32m            line="94"/>[m
     </issue>[m
 [m
     <issue[m
         id="RtlHardcoded"[m
[31m-        message="Use &quot;`start`&quot; instead of &quot;`left`&quot; to ensure correct behavior in right-to-left locales"[m
[31m-        errorLine1="        android:layout_gravity=&quot;left&quot;>"[m
[31m-        errorLine2="                                ~~~~">[m
[32m+[m[32m        message="Use &quot;`start`&quot; instead of &quot;`left`&quot; to ensure correct behavior in right-to-left locales">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="9"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="9"/>[m
     </issue>[m
 [m
     <issue[m
         id="RtlHardcoded"[m
[31m-        message="Consider replacing `android:layout_marginRight` with `android:layout_marginEnd=&quot;3dip&quot;` to better support right-to-left layouts"[m
[31m-        errorLine1="            android:layout_marginRight=&quot;3dip&quot;"[m
[31m-        errorLine2="            ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Consider replacing `android:layout_marginRight` with `android:layout_marginEnd=&quot;3dip&quot;` to better support right-to-left layouts">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="12"[m
[31m-            column="13"/>[m
[32m+[m[32m            line="12"/>[m
     </issue>[m
 [m
     <issue[m
         id="RtlHardcoded"[m
[31m-        message="Use &quot;`start`&quot; instead of &quot;`left`&quot; to ensure correct behavior in right-to-left locales"[m
[31m-        errorLine1="        android:layout_gravity=&quot;left&quot;>"[m
[31m-        errorLine2="                                ~~~~">[m
[32m+[m[32m        message="Use &quot;`start`&quot; instead of &quot;`left`&quot; to ensure correct behavior in right-to-left locales">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="23"[m
[31m-            column="33"/>[m
[32m+[m[32m            line="23"/>[m
     </issue>[m
 [m
     <issue[m
         id="RtlHardcoded"[m
[31m-        message="Consider replacing `android:layout_marginRight` with `android:layout_marginEnd=&quot;3dip&quot;` to better support right-to-left layouts"[m
[31m-        errorLine1="            android:layout_marginRight=&quot;3dip&quot;"[m
[31m-        errorLine2="            ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Consider replacing `android:layout_marginRight` with `android:layout_marginEnd=&quot;3dip&quot;` to better support right-to-left layouts">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="36"[m
[31m-            column="13"/>[m
[32m+[m[32m            line="36"/>[m
     </issue>[m
 [m
     <issue[m
         id="RtlHardcoded"[m
[31m-        message="Consider replacing `android:layout_marginRight` with `android:layout_marginEnd=&quot;3dip&quot;` to better support right-to-left layouts"[m
[31m-        errorLine1="            android:layout_marginRight=&quot;3dip&quot;"[m
[31m-        errorLine2="            ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Consider replacing `android:layout_marginRight` with `android:layout_marginEnd=&quot;3dip&quot;` to better support right-to-left layouts">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="45"[m
[31m-            column="13"/>[m
[32m+[m[32m            line="45"/>[m
     </issue>[m
 [m
     <issue[m
         id="RtlHardcoded"[m
[31m-        message="Consider replacing `android:layout_marginRight` with `android:layout_marginEnd=&quot;3dip&quot;` to better support right-to-left layouts"[m
[31m-        errorLine1="            android:layout_marginRight=&quot;3dip&quot;"[m
[31m-        errorLine2="            ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Consider replacing `android:layout_marginRight` with `android:layout_marginEnd=&quot;3dip&quot;` to better support right-to-left layouts">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="54"[m
[31m-            column="13"/>[m
[32m+[m[32m            line="54"/>[m
     </issue>[m
 [m
     <issue[m
         id="RtlHardcoded"[m
[31m-        message="Consider replacing `android:layout_marginRight` with `android:layout_marginEnd=&quot;3dip&quot;` to better support right-to-left layouts"[m
[31m-        errorLine1="            android:layout_marginRight=&quot;3dip&quot;"[m
[31m-        errorLine2="            ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Consider replacing `android:layout_marginRight` with `android:layout_marginEnd=&quot;3dip&quot;` to better support right-to-left layouts">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="63"[m
[31m-            column="13"/>[m
[32m+[m[32m            line="63"/>[m
     </issue>[m
 [m
     <issue[m
         id="RtlHardcoded"[m
[31m-        message="Consider replacing `android:layout_marginRight` with `android:layout_marginEnd=&quot;3dip&quot;` to better support right-to-left layouts"[m
[31m-        errorLine1="            android:layout_marginRight=&quot;3dip&quot;"[m
[31m-        errorLine2="            ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Consider replacing `android:layout_marginRight` with `android:layout_marginEnd=&quot;3dip&quot;` to better support right-to-left layouts">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="72"[m
[31m-            column="13"/>[m
[32m+[m[32m            line="72"/>[m
     </issue>[m
 [m
     <issue[m
         id="RtlHardcoded"[m
[31m-        message="Consider replacing `android:layout_marginRight` with `android:layout_marginEnd=&quot;3dip&quot;` to better support right-to-left layouts"[m
[31m-        errorLine1="            android:layout_marginRight=&quot;3dip&quot;"[m
[31m-        errorLine2="            ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Consider replacing `android:layout_marginRight` with `android:layout_marginEnd=&quot;3dip&quot;` to better support right-to-left layouts">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="81"[m
[31m-            column="13"/>[m
[32m+[m[32m            line="81"/>[m
     </issue>[m
 [m
     <issue[m
         id="RtlHardcoded"[m
[31m-        message="Consider replacing `android:layout_marginRight` with `android:layout_marginEnd=&quot;3dip&quot;` to better support right-to-left layouts"[m
[31m-        errorLine1="            android:layout_marginRight=&quot;3dip&quot;"[m
[31m-        errorLine2="            ~~~~~~~~~~~~~~~~~~~~~~~~~~">[m
[32m+[m[32m        message="Consider replacing `android:layout_marginRight` with `android:layout_marginEnd=&quot;3dip&quot;` to better support right-to-left layouts">[m
         <location[m
             file="packages/modules/Bluetooth/android/app/res/layout/testactivity_main.xml"[m
[31m-            line="90"[m
[31m-            column="13"/>[m
[32m+[m[32m            line="90"/>[m
     </issue>[m
 [m
 </issues>[m
