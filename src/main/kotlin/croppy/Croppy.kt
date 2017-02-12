package croppy

import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO


class Croppy {
    val STEP = 25

    fun thumb(image: BufferedImage, size: Int): BufferedImage {
        val width = image.width
        val height = image.height

        val squareImage = when {
            width < height -> squareTallImage(image)
            width > height -> squareWideImage(image)
            else -> image
        }

        return resize(squareImage, size)
    }

    private fun resize(original: BufferedImage, size: Int): BufferedImage {
        println("Resizing thumbnail: $size px")
        val scaledBI = BufferedImage(size, size, original.type)
        val g = scaledBI.createGraphics()
        g.drawImage(original, 0, 0, size, size, null)
        g.dispose()
        return scaledBI
    }

    private tailrec fun squareWideImage(image: BufferedImage): BufferedImage =
            if (image.height >= image.width) image
            else squareWideImage(cropWide(image, Math.min(STEP, image.width - image.height)))


    private tailrec fun squareTallImage(image: BufferedImage): BufferedImage =
            if (image.width >= image.height) image
            else squareTallImage(cropTall(image, Math.min(STEP, image.height - image.width)))

    private fun cropWide(image: BufferedImage, amount: Int): BufferedImage {
        println("Cropping $amount pixels")
        val left = image.getSubimage(0, 0, image.width - amount, image.height)
        val right = image.getSubimage(amount, 0, image.width - amount, image.height)

        return if (imageEntropy(right) > imageEntropy(left)) right else left
    }

    private fun cropTall(image: BufferedImage, amount: Int): BufferedImage {
        println("Cropping $amount pixels")
        val top = image.getSubimage(0, 0, image.width, image.height - amount)
        val bottom = image.getSubimage(0, amount, image.width, image.height - amount)

        return if (imageEntropy(top) > imageEntropy(bottom)) top else bottom
    }

    /**
     * Shannon entropy of an image
     */
    private fun imageEntropy(image: BufferedImage): Double {
        var n = 0
        val occ = HashMap<Int, Int>()
        for (i in 0..image.height - 1) {
            for (j in 0..image.width - 1) {
                val pixel = image.getRGB(j, i)

                val red = pixel shr 16 and 0xff
                val green = pixel shr 8 and 0xff
                val blue = pixel and 0xff
                val d = Math.round(0.2989 * red + 0.5870 * green + 0.1140 * blue).toInt()

                if (occ.containsKey(d)) {
                    occ.put(d, occ[d]?.plus(1) as Int)
                } else {
                    occ.put(d, 1)
                }
                ++n
            }
        }
        var e = 0.0
        for ((cx, value) in occ) {
            val p = value.toDouble() / n
            e += p * log2(p)
        }
        return -e
    }

    private fun log2(p: Double): Double = Math.log(p) / 0.6931471805599453


}

fun main(args: Array<String>) {
    val classLoader = Croppy::class.java.classLoader
    val file = File(classLoader.getResource("test/judging-llama.jpg").file)

    println("Reading input image: $file")
    val image: BufferedImage = ImageIO.read(file)

    println("Starting...")
    val croppy: Croppy = Croppy()
    val thumb = croppy.thumb(image, 256)

    val outFile = File("/tmp/out.jpg")
    println("Writing output image: $outFile")
    ImageIO.write(thumb, "jpg", outFile)
}
