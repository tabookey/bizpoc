const path = require('path');
//BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin

module.exports = {
  plugins: [
//      new BundleAnalyzerPlugin()
  ],

  entry: './pubEncrypt.js',
  mode: 'production',
  output: {
    path: path.resolve(__dirname, 'build'),
    filename: 'pubEncrypt.pack.js'
  }
};
