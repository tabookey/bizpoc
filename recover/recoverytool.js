//based on:
// https://github.com/BitGo/wallet-recovery-wizard/blob/master/src/components/non-bitgo.js

bitgo = require( 'bitgo')
Web3=require('web3')

url = "https://kovan.infura.io/v3/c3422181d0594697a38defe7706a1e5b"
web3 = new Web3(new Web3.providers.HttpProvider(url))

  coin='teth'
    let baseCoin = new bitgo.BitGo({env:'test'}).coin(coin)

    const recoveryParams = {
        userKey:'{"iv":"1uY2y4n1514pIstPDbZsOA==","v":1,"iter":10000,"ks":256,"ts":64,"mode":"ccm","adata":"","cipher":"aes","salt":"iYrrO0cbyMs=","ct":"0OnJtSQ23VVag+Cmt2BfPZN8G0sjfwApFCsEWobkIoajWpwC2a7feR1CVd6ImsGiYwulGvktsGBHrAdHzSLDwNmW/i51jqqczAhYyDCm78gYdKgzc6St8Asmdx2eKfEvUA+hvjwJyVNn30kgM3naeyhVDyKCEHY="}',
        backupKey:'{"iv":"mJnseJMGIIADcKvPxrkUnw==","v":1,"iter":10000,"ks":256,"ts":64,"mode":"ccm","adata":"","cipher":"aes","salt":"HJ6U1+S692k=","ct":"LoNUIXSy/8Be049roLFIx4mxDc0iUos++S693Hlm3KNz750X0KD2sKBCnqMkh0jHzP7C2tdO/cPTZHuEcPTXMWsXdubMogS0Aq671LHiffzWH3RaJtwF4Ux8Vf8FRIpbW1U/xXDTMT8SEOBwCcKu/QWozuggNv0="}', 
        walletContractAddress:'0xa246119c346525d863a57ea3e0c95c26b09b7b88',
        tokenAddress:'',
        walletPassphrase:'wallet-pwd2',
        recoveryDestination:'0xd21934eD8eAf27a67f0A70042Af50A1D6d195E81',
        // bitgoKey:'',
        // rootAddress:'',
        // bitgoKey:'',
        // rootAddress:'',
      }
async function myrecover(recoveryParams) {
  try {

      console.log( "balance to recover: ", (await web3.eth.getBalance(recoveryParams.walletContractAddress))/1e18 )
      console.log( "dest balance: ", (await web3.eth.getBalance(recoveryParams.recoveryDestination))/1e18 )
      const recovery = await baseCoin.recover(recoveryParams);
      console.log( "\nrecovery info: "+JSON.stringify(recovery))
      console.log( "sending recovery tx")
      rcpt = await web3.eth.sendSignedTransaction("0x"+recovery.tx)
      console.log( "receipt=",rcpt)

    } catch(e) {
      console.log( "ex",e)
    }
}

myrecover(recoveryParams)
