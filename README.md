-*- mode: Org; -*- (use C-c C-o to follow links in emacs org mode)

* scale_list is
a simple web page that displays scales and fingerings

* Usage
if you have clojure and lein installed, it should suffice to execute the
run script from the top directory

scales should be placed in the scl directory, a huge archive of scales
is available from the scala project

* TODO features [40%]
  - [X] use query strings to select tuning/fundamental
  - [X] use post data to set instrument tuning / fundamental
  - [ ] use cookies
  - [ ] use forms
  - [ ] dynamically generate reference dots
* TODO bugs
**  bugs are logged to  [[file:debug.log][log file]]
  be sure to eliminate old debugging statements
** rendering this is messed up, fix the parser to handle it  [[file:scl/chin_shierlu.scl][shierlu]]
    [[file:src/scale_server/core.clj::defn%20line-mult][in line-mult]]
    [[http://localhost:3000/showscale?scale=chin_shierlu][shierlu in browser]] (warning, pegs CPU with the current version)

* Copyright © 2012 justin smith

