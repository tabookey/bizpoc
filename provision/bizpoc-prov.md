<body>
<title>BizPOC New Customer Provisioning Flow</title>
<H1>BizPOC New Customer Provisioning Flow</H1>

<h3>Prerequisites:</h3> 
<ul>
<li> Run this script on a TRUSTED host, and only from a local file (NOT form the network)
<li> Must have AUDITOR present.
<li> Must have Bitgo accounts for all guardians, each with his own Yubikey.
</ul>
<p>

<h3>Flow</h3>
<nl>

<script>
    //from: http://jsfiddle.net/epinapala/WdeTM/4/
    function selectText(element) {
    var doc = document;
    var text = doc.getElementById(element);    

    if (doc.body.createTextRange) { // ms
        var range = doc.body.createTextRange();
        range.moveToElementText(text);
        range.select();
    } else if (window.getSelection) { // moz, opera, webkit
        var selection = window.getSelection();            
        var range = doc.createRange();
        range.selectNodeContents(text);
        selection.removeAllRanges();
        selection.addRange(range);
    }
}

    function genpwd(id) {
        var item = document.getElementById(id);    
        item.innerText="bad-random-"+Math.floor(Math.random()*1e18).toString(36)
        selectText(id)
        document.execCommand('copy')
    }

    function genActivationKey(id) {
        var item = document.getElementById(id);    
        var accesstoken = document.getElementById("accesstoken").value;
        var walletpwd = document.getElementById("wpwd");    
        key = Math.floor(Math.random()*1e16).toString().replace(/(\d\d\d\d)\B/g, "$1-")
        selectText(id)

        item.innerText = "\nactivation key: "+key+"\n\n Data sent to server:"+
            "\n    accesstoken:"+accesstoken +
            "\n    wpwd:"+walletpwd.innerText
    }
</script>


1. <input type=submit value="Generate client password (in clipboard)"  onclick="genpwd('pwd')"> -  <font size=1><span id=pwd></span> </font>
<font size=1>

    * password used by customer's web user, during provisioning only.
    * not saved anywhere, since we're generating "Access key" for the app.
    * can't be used later (by admin), since login requires Yubikey.

</font>

3. In *incognito* window, open https://www.bitgo.com/info/signup
4. Register customer:
    - use email with auto-forward to admin's email.
        - e.g: dror+custname@tabookey.com (Google's auto-forward to dror@tabookey.com) 
    - (Enter full name of customer)
    - paste password from clipboard

5. Close window.
6. Open "Bitgo Email Verification". Right-click on "Click to Verify" and open in incognito window again.
7. Paste password again to login

   * (password is not needed anymore, unless the incognito window is closed)

7. Add a Yubikey device to user.

8. <input type=submit value="Generate wallet password (in clipboard)"  onclick="genpwd('wpwd')"> - <font size=1><span id=wpwd></span> </font>
9. Open https://www.bitgo.com/login as **ADMINISTRATOR**
10. Invite customer's email to enterprise.
11. Switch to Customer's incognito window to accept invitation.
11. Create new wallet:

    * Coin: **Ethereum**, click "Next"
    * **Quick Setup**, click "Next"
    * Wallet name: (customer name)
    * Click: **Secondary Password**. 
    * Paste from clipboard *wallet password* (twice), create wallet.
    * Print **Bitgo Keycard PDF**
    * Enter activation code (from PDF) to activate wallet
    * Click <a href="javascript:'<H1>Wallet Password</H1><p>\n'+wpwd.innerText" target="print">Print Wallet Password</a>

12. Invite guardians to wallet with role "Admin"
    * must use "Select people in your organization", and NOT "Invite new users" for both guardians and customer
    * requires *wallet password*. Its still in the clipboard, so paste it.
    * make sure guardians accept invitation to join wallet.
13. Invite user to wallet with role "Spender"
    - switch to the customer's incognito window, to accept invitation to join wallet.
    
14. Create Customer's Access-Token:
    - in customer's window, top-right menu, User-Settings
    - Select, "Developer Options" and then "+ Add Access Token"
    - give it a Label
    - scroll to the end of the page (very long..)
    - Enter "IP Address Allowed": `35.177.187.77`
    - Check View, Spend, Full-Administrative-Access and "agree"
    - (that is, all checkboxes except "create wallets")
    - Click "Add Token", enter Yubikey
    - copy the on-screen access token
    - Paste access-token here: <input id=accesstoken placeholder="{paste}">
    
14. Change wallet Policy (can only be done after above users accepted invitations)
    * Approve All Outgoing Transactions
    * Require Multiple (2) Admin Approvals
    
15. Upload to server:
    * Put in **ADMINISTRATOR's** Yubikey OTP: <input id=adminotp>
    * Put in *Customer's* Yubikey OTP-ID: <input id=otp>

      - Script below uses above *wallet password*
      - Customer's Yubikey not verified here: can put id (12chars) or entire key

    * Click <input type=submit value="Upload User Info" onclick="genActivationKey('activation')"> - <font size=1><span id=activation></span></font>
    * Save above activation key (until user receives Yubikey) 

16. Send Yubikey to user.

### After Yubikey is received by customer:

* Make video call. 
* Validate customer
* Customer must enable fingerprint on device.
* Let customer install App (from Google Play)
* Application: 
    - App requires user to enter Yubikey
    - Guardian gives **activation key** to customer on the phone
    - Customer enters **activation key**
    - App verifies `hash(yubikey-id, activation-key) == activation-hash`
        (if not, user mis-typed the code)
    <font size=1>
    - Server verifies hash, verifies yubikey otp.
    - Server returns (and forgets) encrypted-client-info
    - application run `info = aes-decode(encrypted-client-info, scrypt(activation-key))`
    - application configures witn above info.
    </font>
    - App says "**Woohoo!**"
* *Perform first transaction with client*

</nl>


</body>