REM get the name of the file right for the release
tar --create --gzip --file=tunnel2012-12.tar.gz --transform='s:^:tunnel2012-12/:' src symbols tutorials b.bat j.bat
REM then add this to https://bitbucket.org/goatchurch/tunnelx/downloads
