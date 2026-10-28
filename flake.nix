{
  description = "Royal Titles - Forge 1.20.1 mod";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs = { self, nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = nixpkgs.legacyPackages.${system};
      # Minecraft 1.20.1 / Forge 47 target Java 17; ForgeGradle 6 needs Gradle 8
      jdk = pkgs.jdk17;
    in {
      # Build-only shell. Run `./gradlew runClient` from the host shell instead:
      # Nix's libGL can't load the host's GPU drivers on non-NixOS distros.
      devShells.${system}.default = pkgs.mkShell {
        packages = [ jdk pkgs.git (pkgs.gradle_8.override { java = jdk; }) ];
        JAVA_HOME = jdk.home;
        # interactive `nix develop` drops into zsh; `nix develop -c` and direnv stay non-interactive
        shellHook = ''
          if [[ $- == *i* ]] && command -v zsh >/dev/null; then exec zsh; fi
        '';
      };
    };
}
