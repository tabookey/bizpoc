const BitGoJS = require('bitgo');
const bitgo = new BitGoJS.BitGo({ env: 'prod' });
const xprv = 'xprv9s21ZrQH143K4YNpbiHKKvA5Lhwq8dZemhynHWaiLS8gsTgq1CZem7Kyd3fHeLHiWge1cw49CYfpPEBMCN4osFBX8Ri75myVrxQaHCLpDrg';
const password = 'jesuschristthisisannoying';
const derivedKey = bitgo.coin('eth').deriveKeyWithSeed({ key: xprv, seed: '123' });
const blob = bitgo.encrypt({ input: derivedKey.key, password });
console.log(blob);
console.log("Done");
