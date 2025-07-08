![image](https://user-images.githubusercontent.com/677254/66143298-d9cc2780-e5fe-11e9-9693-1315bb53846b.png)

# Introduction #

Tunnel is a [free](http://www.gnu.org/) [Java](http://sun.java.net/) 2.5D cave drawing program surveys based on [Survex](http://www.survex.com/)-compatible data which can also read and [PocketTopo](http://paperless.bheeb.ch/) files.

It is used primarily to draw up [CUCC Austria caves](http://expo.survex.com/) and the [Three Counties System](http://cave-registry.org.uk/nengland).  

The defining feature (ie why you cannot use a standard drawing software for this application) is the ability to distort the maps to fit to changes to the underlying survey network.  

The main alternative to Tunnel is [Therion](http://therion.sk), which also solves this problem, but in a completely different manner and with a slightly steeper learning curve.

# Quickstart #

Download `tunnel2019a.jar` from the [downloads page](https://github.com/CaveSurveying/tunnelx/releases) and double-click on it.

Read the [inline help file](https://github.com/CaveSurveying/tunnelx/blob/master/symbols/helpfile.md) online or from within the program.

# Nix #

Hoping to get this program into the nix repository.  For now we can build it using this code:

```
nix-build --verbose --expr 'with import <nixpkgs> {}; callPackage ./tunnel_attempt.nix {}'
```

Or even better we can do:
nix-shell --verbose  tunnel_attempt2.nix
nix-build --verbose  tunnel_attempt2.nix
