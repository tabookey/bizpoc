# Provisioning flow:

### Pre-requisites:
- Create admin (guardian) accounts on bitgo
- **On test.bitgo.com**: also create an "Enterprise", and invite guardians to it.
- **On production**: Need sales rep. to create the enterprise.

### Enlist New Customer:

- Create customer account with "customer name", and email dror+customerName@tabookey.com
    - use good password: this is the wallet password used in our client's provisioning URL.
- go to email to verify it.
- add 2FA (google auth, yubikey?)

- (From another window) Login as admin bitgo account
- From Top/left menu: Switch to organization. 
- From Top/left menu: Manage Organization/Users
- Invite User. enter above customer email.
- From customer window: accept invitation.


- From admin window:
- Wallets/Create Wallet
- Quick-Setup, Next.
- Give wallet the customer's name, Next
- Secondary Password: enter a wallet password (for this admin)
    - can re-use the user's password 
    - (our private key is encryped separately for each user)
- "For small Balances", next (TODO: use our own xpub backup key)
- Activate wallet (with key from PDF), click "activate"
- "Add additional User"
    - add customer's account as "spender"
    - add guardians as "admin"
    - NOTE: you need wallet password to add participants.

- Each guardian need to login and accept to join the wallet.
- Can allow "n" signers (sender+approvers) when there are at least "n+1" users on a vault.
- After all guardians accepted:
    - open wallet, Policy
    - require 2 admins to approve

On Customer window:
- accept join to wallet.
- Top-left menu: User Settings/Developer Options/ +Add Access Token
- Sscroll to bottom. check "View", "Spend", "Full admin" (and "agree")
    (note that its "full admin rights" for the account - still only a spender on the wallet)
- Click "Add Token"
- Copy token string.

- Open local bitgo/provision/files/bitgo-qr.html
    OR: https://gsn.tabookey.com/bizpoc/bizpoc-qr.html
- Paste access token copied above. 
- paste the customer's password.
- click "tab" to make sure QRcode is updated.


### Provision app:

- Enlist to "internal test" https://play.google.com/apps/internaltest/4700328908680150302
- Install app from: https://play.google.com/store/apps/details?id=com.tabookey.bizpoc

- Start client app
- Scan QRcode.
    - NOTE: If QR code is scanned, it means its fully functional. The only reason for NOT making transaction is policy rules on bitgo.
