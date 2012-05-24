-*- mode: Org; -*- (use C-c C-o to follow links in emacs org mode)

* scale_list is
a simple web page that displays scales and fingerings

* Usage
if you have clojure and lein installed, it should suffice to execute the
run script from the top directory

scales should be placed in the scl directory, a huge archive of scales
is avalable from the scala project

* TODO features
** use post data to set instrument tuning / reference points / fundamental
*** first we will use query string, then figure out post
    query string done, now for post

* TODO bugs
**  bugs are logged to  [[file:debug.log][log file]]
  be sure to eliminate old debugging statements
** redering this is messed up, fix the parser to handle it  [[file:scl/chin_shierlu.scl][shierlu]]
    [[file:src/scale_server/core.clj::defn%20line-mult][in line-mult]]

* Copyright © 2012 justin smith

