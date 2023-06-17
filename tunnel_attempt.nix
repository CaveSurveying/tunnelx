{
  lib
, stdenv
, fetchFromGitHub
, jdk
, jre
, fetchgit
, survex
, makeWrapper
, bash
, coreutils
}:
stdenv.mkDerivation rec {
  pname = "tunnelx";
  version = "2023-07";

  src = fetchFromGitHub {
    owner = "CaveSurveying";
    repo = "tunnelx";
    rev = "ad653f0d6e7e1b1823204047c88e8ab917931957";
    hash = "sha256-ktEhxmQHOBUU+z9Sls9FDUe3M2iupC5BEciOAY0JI28=";
  };


  buildInputs = [
    jdk
    makeWrapper
  ];

  outputs = [ "out" ];
  
  runtimeInputs = [ 
    survex 
  ];
  
  configurePhase = ''
  '';

  buildPhase = ''
    ls -1 symbols > symbols/listdir.txt
    ls -1 tutorials > tutorials/listdir.txt
    javac -d . src/*.java
    jar cmf tunnelmanifest.txt tunnel2023nix.jar Tunnel \
        symbols/*.xml symbols/*.html symbols/helpfile.md \
        symbols/listdir.txt tutorials/*.* tutorials/listdir.txt
  '';

  installPhase = ''
    mkdir -p $out/bin
    mkdir -p $out/java
    cp tunnel2023nix.jar $out/java
    makeWrapper ${jre}/bin/java $out/bin/tunnelx \
        --add-flags "-jar $out/java/tunnel2023nix.jar" \
        --set SURVEX_EXECUTABLE_DIR ${survex}/bin/
  '';

  meta = with lib; {
    description = "TunnelX – cave surveying software";
    homepage = "https://github.com/CaveSurveying/tunnelx/";
    changelog = "https://github.com/CaveSurveying/tunnelx/blob/${src.rev}/CHANGES";
    license = licenses.gpl2;
    maintainers = with maintainers; [ goatchurchprime ];
  };
}
