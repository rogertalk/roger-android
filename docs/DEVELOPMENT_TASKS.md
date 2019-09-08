# Testing install referrer

It's possible to simulate the intent sent by the Play Store the the application upon install from a link with metadata.
To do so access the device's shell and type: 

    am broadcast -a com.android.vending.INSTALL_REFERRER  -n com.rogertalk.roger/com.rogertalk.roger.android.receivers.ReferrerReceiver --es referrer "eyJwdWJsaWNfcHJvZmlsZSI6IjU2OTU0MTQ2NjU3NDAyODgiLCJjaHVua190b2tlbiI6IkN2TGJCIiwiZGVmYXVsdF9pZGVudGlmaWVyIjoiKzExMzA1NTUwMTI2In0="

`referrer` is a BASE64 encoded JSON string, that looks like the following:

    {"public_profile":"5695414665740288","chunk_token":"CvLbB","default_identifier":"+11305550126"}

# Testing Roger's URI
 
 When visiting a page profile, there's a specific URI for the app to consume ( https://rogertalk.com/pedro ). To simulate such link one can open a webapge or do it trough the device's shell like this:
 
 
For **V2**:
 
   am start -a android.intent.action.VIEW -d "rogertalk://v2/open?chunk_token=pFglC&display_name=Pedro&id=5695414665740288&image_url=https%3A%2F%2Fapi.rogertalk.com%2Ffile%2Fdab88cdd3274581551ea1f859c9dcaa6d0d7523755366e19aace21707350019f-p.jpg"
   
   
For **V3**:
 
   am start -a android.intent.action.VIEW -d "rogertalk://v3/open?profile=%7B%22id%22%3A5695414665740288%2C%22display_name%22%3A%22Pedro%22%2C%22image_url%22%3A%22https%3A%2F%2Fapi.rogertalk.com%2Ffile%2Fdab88cdd3274581551ea1f859c9dcaa6d0d7523755366e19aace21707350019f-p.jpg%22%2C%22audio_url%22%3A%22https%3A%2F%2Fapi.rogertalk.com%2Ffile%2Ff09145261e10e6d4b236910484786617402ed74cfeb280cc5450df1b5d940284-p.mp3%22%2C%22duration%22%3A1500%2C%22chunk_token%22%3A%22pFglC%22%7D&refresh_token=nsm8Mgb2PTF8Tsr9diUpuiaKSapUaInnPg9c"

# Test IFTTT DeepLink

  am start "intent:#Intent;scheme=rogerbot://rgr.im/ifttt;end"

