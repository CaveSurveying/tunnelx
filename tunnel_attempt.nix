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
    javac -d . src/*.java
  '';

  installPhase = ''
    mkdir -p $out/bin
    mkdir -p $out/java
    cp -r Tunnel $out/java
    cp -r symbols $out/java
    cp -r tutorials $out/java
    makeWrapper ${jre}/bin/java $out/bin/tunnelx \
        --add-flags "-cp $out/java Tunnel.MainBox" \
        --set SURVEX_EXECUTABLE_DIR ${survex}/bin/
        --set TUNNEL_USER_DIR ${out}/java/
  '';

  meta = with lib; {
    description = "TunnelX – cave surveying software";
    homepage = "https://github.com/CaveSurveying/tunnelx/";
    changelog = "https://github.com/CaveSurveying/tunnelx/blob/${src.rev}/CHANGES";
    license = licenses.gpl2;
    maintainers = with maintainers; [ goatchurchprime ];
  };
}
