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
}));

config.devServer = {
    open: false,
    port: 3000,
    proxy: {
        "/api": "http://localhost:8080",
        "/kvws/*": {
            "target": "ws://localhost:8080",
            "ws": true
        }
    },
    static: [
      __dirname + "/../build/processedResources/js/main"
    ],
    historyApiFallback: {
        index: "/static/index.html",
        rewrites: [
            {
                from: /^\/api.*$/,
                to: function (context) {
                    return context.parsedUrl.pathname;
                }
            }
        ],
        logger: console.log.bind(console)
    }
};

config.output = config.output || {};
config.output.publicPath = "/static/";
