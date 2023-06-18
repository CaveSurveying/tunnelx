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
    rev = "8c455ec27e4e9ea754cd87a4e2e0f0b2bc558b88";
    hash = "sha256-r70ZmrpN7I/bsvHAxE7uRaryqNlz/z9zA55qiCC8NkE=";
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
        --set SURVEX_EXECUTABLE_DIR ${survex}/bin/ \
        --set TUNNEL_USER_DIR $out/java/
  '';

  meta = with lib; {
    description = "TunnelX – cave surveying software";
    homepage = "https://github.com/CaveSurveying/tunnelx/";
    changelog = "https://github.com/CaveSurveying/tunnelx/blob/${src.rev}/CHANGES";
    license = licenses.gpl2;
    maintainers = with maintainers; [ goatchurchprime ];
  };
}
