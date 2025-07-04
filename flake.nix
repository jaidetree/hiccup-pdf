{
  description = "ClojureScript Hiccup PDF";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, flake-utils, nixpkgs }: flake-utils.lib.eachDefaultSystem (system:
    let
      pkgs = import nixpkgs { inherit system; };
    in
    {
      devShell = pkgs.mkShell {
        buildInputs = [
          pkgs.babashka
          pkgs.clj-kondo
          pkgs.clojure
          pkgs.clojure-lsp
          pkgs.nodejs_22
          pkgs.temurin-jre-bin-17
        ];
      };
    });
}
