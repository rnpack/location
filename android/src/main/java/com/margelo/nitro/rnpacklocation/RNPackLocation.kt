package com.margelo.nitro.rnpacklocation
  
import com.facebook.proguard.annotations.DoNotStrip

@DoNotStrip
class RNPackLocation : HybridRNPackLocationSpec() {
  override fun multiply(a: Double, b: Double): Double {
    return a * b
  }
}
