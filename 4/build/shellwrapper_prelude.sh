#!/bin/sh
# from https://possiblelossofprecision.net/?p=2235
MYSELF=`which "$0" 2>/dev/null`
[ $? -gt 0 -a -f "$0" ] && MYSELF="./$0"
java=java
if test -n "$JAVA_HOME"; then
    java="$JAVA_HOME/bin/java"
fi
exec "$java" -Djava.security.egd=file:/dev/./urandom -noverify -cp $MYSELF __MAINCLASS__ "$@"
exit 1
