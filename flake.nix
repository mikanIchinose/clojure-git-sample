{
  description = "clojure-git-sample";
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
    flake-parts.url = "github:hercules-ci/flake-parts";
    treefmt-nix = {
      url = "github:numtide/treefmt-nix";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    systems.url = "github:nix-systems/default";
  };
  outputs =
    inputs:
    inputs.flake-parts.lib.mkFlake { inherit inputs; } {
      systems = import inputs.systems;
      imports = [ inputs.treefmt-nix.flakeModule ];
      perSystem =
        { pkgs, config, ... }:
        {
          packages.default = pkgs.hello;
          devShells.default = pkgs.mkShell {
            packages = with pkgs; [
              graalvm-ce
              (clojure.override { jdk = graalvm-ce; })
            ];
            shellHook = ''
              zsh
            '';
          };
          treefmt = {
            programs.nixfmt.enable = true;
            programs.cljfmt.enable = true;
          };
        };
    };
}
