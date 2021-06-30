<?php
  header( 'Location: com.github.kaiwinter.nfcsonos://callback' . $_SERVER['REQUEST_URI'] ) ;
?>
<html>
  <head>
    <meta HTTP-EQUIV="REFRESH" content="0; url=com.github.kaiwinter.nfcsonos://callback<?=$_SERVER['REQUEST_URI']?>">
  </head>
  <body>
    <h1>Authorization finished</h1>
    <h2>
      <a href="com.github.kaiwinter.nfcsonos://callback<?=$_SERVER['REQUEST_URI']?>">Go back to NFC Controller for Sonos</a>
    </h2>
  </body>
</html>