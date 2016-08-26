package org.bitcoins.spvnode.serializers.control

import org.bitcoins.core.number.{UInt32, UInt64}
import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.util.BitcoinSUtil
import org.bitcoins.spvnode.bloom.BloomUpdateNone
import org.scalatest.{FlatSpec, MustMatchers}

/**
  * Created by chris on 7/20/16.
  */
class RawFilterLoadMessageSerializerTest extends FlatSpec with MustMatchers  {

  "RawFilterLoadMessageSerializer" must "deserialize and serialize a filter load message" in {
    //example from the bitcoin developer reference
    //https://bitcoin.org/en/developer-reference#filterload
    val hex = "02b50f0b0000000000000000"

    val filterLoadMsg = RawFilterLoadMessageSerializer.read(hex)
    filterLoadMsg.filterSize must be (CompactSizeUInt(UInt64(2)))
    BitcoinSUtil.encodeHex(filterLoadMsg.filter) must be ("b50f")
    filterLoadMsg.hashFuncs must be (UInt32(11))
    filterLoadMsg.tweak must be (UInt32.zero)
    filterLoadMsg.flags must be (BloomUpdateNone)

    RawFilterLoadMessageSerializer.write(filterLoadMsg) must be (hex)

  }
}