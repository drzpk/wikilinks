const HtmlWebpackPlugin = require("html-webpack-plugin");

config.resolve.modules.push("../../processedResources/js/main");

if (config.devServer) {
    config.devServer.hot = true;
    config.devtool = 'eval-cheap-source-map';
} else {
    config.devtool = undefined;
}

// disable bundle size warning
config.performance = {
    assetFilter: function (assetFilename) {
      return !assetFilename.endsWith('.js');
    },
};

config.plugins.push(new HtmlWebpackPlugin({
    template: "kotlin/index.template.html",
    output: {
        filename: "index.html"
    },
    minify: {
        removeComments: false // Required for analytics to work (placeholder comments)
    }
}))