package com.noeupapp.middleware.packages

import com.noeupapp.middleware.packages.pack.Pack


/**
  * Created by damien on 28/02/2017.
  */
trait ActionPackage {

  def packages(action: String): Set[Pack]

}
