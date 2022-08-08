config.module.rules.push({ test: /\.css$/, use: ["style-loader", { loader: "css-loader", options: {sourceMap: false} } ] });

// https://webpack.js.org/loaders/sass-loader/
config.module.rules.push({
   test: /\.s[ac]ss$/i,
   use: [
      // Creates `style` nodes from JS strings
      "style-loader",
      // Translates CSS into CommonJS
      "css-loader",
      // Compiles Sass to CSS
      "sass-loader",
   ],
});
