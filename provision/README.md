# Provisioning a new wallet:

- **NOTE:** Assume already have bitgo accounts for guardians (Liraz, Liraz2, Konfidas)

### New Client account:
1. Use private email e.g liraz+CustomerName@tabookey.com
2. Settings/Developer/Access-Token. allow "view" and "spend". Save token.
3. Logout.

### From Liraz Account
1. Create new wallet. use good passphrase.
2. Assign account with 2FA (Yubikey)
3. Invite guardians (as admin): Konfidas, Liraz2
4. Invite customer account as "spender"


#### Login as (client,Liraz2) to accept invitations.


### QRCode:
1. Run "bizpoc-qr.html" 
    (uploaded as https://gsn.tabookey.com/bizpoc/bizpoc-qr.html)
2. *Turn off networking*
3. Paste passphrase
4. Paste access token.
5. Print generated qrcode.
6. delete local file.
