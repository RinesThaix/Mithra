package sexy.kostya.mithra.bridge

interface Block {

    val lightEmission: Int
    val opaque: Boolean
    val propagatesSkylightDown: Boolean

}