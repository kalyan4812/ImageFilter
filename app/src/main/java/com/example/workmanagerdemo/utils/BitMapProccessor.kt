import android.graphics.*
import android.media.ExifInterface
import java.io.IOException
import java.util.*

object BitMapProccessor {
    // [-360, +360] -> Default = 0
    fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun flip(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
        val matrix = Matrix()
        matrix.preScale(if (horizontal) -1f else 1.toFloat(), if (vertical) -1f else 1.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun emboss(src: Bitmap): Bitmap {
        val EmbossConfig = arrayOf(
            doubleArrayOf(-1.0, 0.0, -1.0),
            doubleArrayOf(0.0, 4.0, 0.0),
            doubleArrayOf(-1.0, 0.0, -1.0)
        )
        val convMatrix = ConvolutionMatrix(3)
        convMatrix.applyConfig(EmbossConfig)
        convMatrix.Factor = 1.0
        convMatrix.Offset = 127.0
        return ConvolutionMatrix.computeConvolution3x3(src, convMatrix)
    }

    // [0, 150], default => 100
    fun cfilter(src: Bitmap?, red: Double, green: Double, blue: Double): Bitmap {
        var src = src
        var red = red
        var green = green
        var blue = blue
        red = red / 100
        green = green / 100
        blue = blue / 100

        // image size
        val width = src!!.width
        val height = src.height
        // create output bitmap
        val bmOut = Bitmap.createBitmap(width, height, src.config)
        // color information
        var A: Int
        var R: Int
        var G: Int
        var B: Int
        var pixel: Int

        // scan through all pixels
        for (x in 0 until width) {
            for (y in 0 until height) {
                // get pixel color
                pixel = src.getPixel(x, y)
                // apply filtering on each channel R, G, B
                A = Color.alpha(pixel)
                R = (Color.red(pixel) * red).toInt()
                G = (Color.green(pixel) * green).toInt()
                B = (Color.blue(pixel) * blue).toInt()
                // set new color pixel to output bitmap
                bmOut.setPixel(x, y, Color.argb(A, R, G, B))
            }
        }
        src.recycle()
        src = null

        // return final image
        return bmOut
    }

    fun gaussian(src: Bitmap): Bitmap {
        val GaussianBlurConfig = arrayOf(
            doubleArrayOf(1.0, 2.0, 1.0),
            doubleArrayOf(2.0, 4.0, 2.0),
            doubleArrayOf(1.0, 2.0, 1.0)
        )
        val convMatrix = ConvolutionMatrix(3)
        convMatrix.applyConfig(GaussianBlurConfig)
        convMatrix.Factor = 16.0
        convMatrix.Offset = 0.0
        return ConvolutionMatrix.computeConvolution3x3(src, convMatrix)
    }

    // bitOffset = (16, 32, 64, 128)
    fun cdepth(src: Bitmap?, bitOffset: Int): Bitmap {
        // get image size
        var src = src
        val width = src!!.width
        val height = src.height
        // create output bitmap
        val bmOut = Bitmap.createBitmap(width, height, src.config)
        // color information
        var A: Int
        var R: Int
        var G: Int
        var B: Int
        var pixel: Int

        // scan through all pixels
        for (x in 0 until width) {
            for (y in 0 until height) {
                // get pixel color
                pixel = src.getPixel(x, y)
                A = Color.alpha(pixel)
                R = Color.red(pixel)
                G = Color.green(pixel)
                B = Color.blue(pixel)

                // round-off color offset
                R = R + bitOffset / 2 - (R + bitOffset / 2) % bitOffset - 1
                if (R < 0) {
                    R = 0
                }
                G = G + bitOffset / 2 - (G + bitOffset / 2) % bitOffset - 1
                if (G < 0) {
                    G = 0
                }
                B = B + bitOffset / 2 - (B + bitOffset / 2) % bitOffset - 1
                if (B < 0) {
                    B = 0
                }

                // set pixel color to output bitmap
                bmOut.setPixel(x, y, Color.argb(A, R, G, B))
            }
        }
        src.recycle()
        src = null
        return bmOut
    }

    fun sharpen(src: Bitmap): Bitmap {
        val SharpConfig = arrayOf(
            doubleArrayOf(0.0, -2.0, 0.0),
            doubleArrayOf(-2.0, 11.0, -2.0),
            doubleArrayOf(0.0, -2.0, 0.0)
        )
        val convMatrix = ConvolutionMatrix(3)
        convMatrix.applyConfig(SharpConfig)
        convMatrix.Factor = 3.0
        return ConvolutionMatrix.computeConvolution3x3(src, convMatrix)
    }

    fun noise(source: Bitmap?): Bitmap {
        var source = source
        val COLOR_MAX = 0xFF

        // get image size
        val width = source!!.width
        val height = source.height
        val pixels = IntArray(width * height)
        // get pixel array from source
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        // a random object
        val random = Random()
        var index = 0
        // iteration through pixels
        for (y in 0 until height) {
            for (x in 0 until width) {
                // get current index in 2D-matrix
                index = y * width + x
                // get random color
                val randColor = Color.rgb(
                    random.nextInt(COLOR_MAX),
                    random.nextInt(COLOR_MAX), random.nextInt(COLOR_MAX)
                )
                // OR
                pixels[index] = pixels[index] or randColor
            }
        }
        // output bitmap
        val bmOut = Bitmap.createBitmap(width, height, source.config)
        bmOut.setPixels(pixels, 0, width, 0, 0, width, height)
        source.recycle()
        source = null
        return bmOut
    }

    // [-255, +255] -> Default = 0
    fun brightness(src: Bitmap?, value: Int): Bitmap {
        // image size
        var src = src
        val width = src!!.width
        val height = src.height
        // create output bitmap
        val bmOut = Bitmap.createBitmap(width, height, src.config)
        // color information
        var A: Int
        var R: Int
        var G: Int
        var B: Int
        var pixel: Int

        // scan through all pixels
        for (x in 0 until width) {
            for (y in 0 until height) {
                // get pixel color
                pixel = src.getPixel(x, y)
                A = Color.alpha(pixel)
                R = Color.red(pixel)
                G = Color.green(pixel)
                B = Color.blue(pixel)

                // increase/decrease each channel
                R += value
                if (R > 255) {
                    R = 255
                } else if (R < 0) {
                    R = 0
                }
                G += value
                if (G > 255) {
                    G = 255
                } else if (G < 0) {
                    G = 0
                }
                B += value
                if (B > 255) {
                    B = 255
                } else if (B < 0) {
                    B = 0
                }

                // apply new pixel color to output bitmap
                bmOut.setPixel(x, y, Color.argb(A, R, G, B))
            }
        }
        src.recycle()
        src = null
        return bmOut
    }

    fun sepia(src: Bitmap?): Bitmap {
        // image size
        var src = src
        val width = src!!.width
        val height = src.height
        // create output bitmap
        val bmOut = Bitmap.createBitmap(width, height, src.config)
        // constant grayscale
        val GS_RED = 0.3
        val GS_GREEN = 0.59
        val GS_BLUE = 0.11
        // color information
        var A: Int
        var R: Int
        var G: Int
        var B: Int
        var pixel: Int

        // scan through all pixels
        for (x in 0 until width) {
            for (y in 0 until height) {
                // get pixel color
                pixel = src.getPixel(x, y)
                // get color on each channel
                A = Color.alpha(pixel)
                R = Color.red(pixel)
                G = Color.green(pixel)
                B = Color.blue(pixel)
                // apply grayscale sample
                R = (GS_RED * R + GS_GREEN * G + GS_BLUE * B).toInt()
                G = R
                B = G

                // apply intensity level for sepid-toning on each channel
                R += 110
                if (R > 255) {
                    R = 255
                }
                G += 65
                if (G > 255) {
                    G = 255
                }
                B += 20
                if (B > 255) {
                    B = 255
                }

                // set new pixel color to output image
                bmOut.setPixel(x, y, Color.argb(A, R, G, B))
            }
        }
        src.recycle()
        src = null
        return bmOut
    }

    // red, green, blue [0, 48]
    fun gamma(src: Bitmap?, red: Double, green: Double, blue: Double): Bitmap {
        var src = src
        var red = red
        var green = green
        var blue = blue
        red = (red + 2) / 10.0
        green = (green + 2) / 10.0
        blue = (blue + 2) / 10.0
        // create output image
        val bmOut = Bitmap.createBitmap(src!!.width, src.height, src.config)
        // get image size
        val width = src.width
        val height = src.height
        // color information
        var A: Int
        var R: Int
        var G: Int
        var B: Int
        var pixel: Int
        // constant value curve
        val MAX_SIZE = 256
        val MAX_VALUE_DBL = 255.0
        val MAX_VALUE_INT = 255
        val REVERSE = 1.0

        // gamma arrays
        val gammaR = IntArray(MAX_SIZE)
        val gammaG = IntArray(MAX_SIZE)
        val gammaB = IntArray(MAX_SIZE)

        // setting values for every gamma channels
        for (i in 0 until MAX_SIZE) {
            gammaR[i] = Math.min(
                MAX_VALUE_INT,
                (MAX_VALUE_DBL * Math.pow(i / MAX_VALUE_DBL, REVERSE / red) + 0.5).toInt()
            )
            gammaG[i] = Math.min(
                MAX_VALUE_INT,
                (MAX_VALUE_DBL * Math.pow(i / MAX_VALUE_DBL, REVERSE / green) + 0.5).toInt()
            )
            gammaB[i] = Math.min(
                MAX_VALUE_INT,
                (MAX_VALUE_DBL * Math.pow(i / MAX_VALUE_DBL, REVERSE / blue) + 0.5).toInt()
            )
        }

        // apply gamma table
        for (x in 0 until width) {
            for (y in 0 until height) {
                // get pixel color
                pixel = src.getPixel(x, y)
                A = Color.alpha(pixel)
                // look up gamma
                R = gammaR[Color.red(pixel)]
                G = gammaG[Color.green(pixel)]
                B = gammaB[Color.blue(pixel)]
                // set new color to output bitmap
                bmOut.setPixel(x, y, Color.argb(A, R, G, B))
            }
        }
        src.recycle()
        src = null

        // return final image
        return bmOut
    }

    // [-100, +100] -> Default = 0
    fun contrast(src: Bitmap?, value: Double): Bitmap {
        // image size
        var src = src
        val width = src!!.width
        val height = src.height
        // create output bitmap

        // create a mutable empty bitmap
        val bmOut = Bitmap.createBitmap(width, height, src.config)

        // create a canvas so that we can draw the bmOut Bitmap from source bitmap
        val c = Canvas()
        c.setBitmap(bmOut)

        // draw bitmap to bmOut from src bitmap so we can modify it
        c.drawBitmap(src, 0f, 0f, Paint(Color.BLACK))


        // color information
        var A: Int
        var R: Int
        var G: Int
        var B: Int
        var pixel: Int
        // get contrast value
        val contrast = Math.pow((100 + value) / 100, 2.0)

        // scan through all pixels
        for (x in 0 until width) {
            for (y in 0 until height) {
                // get pixel color
                pixel = src.getPixel(x, y)
                A = Color.alpha(pixel)
                // apply filter contrast for every channel R, G, B
                R = Color.red(pixel)
                R = (((R / 255.0 - 0.5) * contrast + 0.5) * 255.0).toInt()
                if (R < 0) {
                    R = 0
                } else if (R > 255) {
                    R = 255
                }
                G = Color.green(pixel)
                G = (((G / 255.0 - 0.5) * contrast + 0.5) * 255.0).toInt()
                if (G < 0) {
                    G = 0
                } else if (G > 255) {
                    G = 255
                }
                B = Color.blue(pixel)
                B = (((B / 255.0 - 0.5) * contrast + 0.5) * 255.0).toInt()
                if (B < 0) {
                    B = 0
                } else if (B > 255) {
                    B = 255
                }

                // set new pixel color to output bitmap
                bmOut.setPixel(x, y, Color.argb(A, R, G, B))
            }
        }
        src.recycle()
        src = null
        return bmOut
    }

    // [0, 200] -> Default = 100
    fun saturation(src: Bitmap?, value: Int): Bitmap {
        var src = src
        val f_value = (value / 100.0).toFloat()
        val w = src!!.width
        val h = src.height
        val bitmapResult = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvasResult = Canvas(bitmapResult)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(f_value)
        val filter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = filter
        canvasResult.drawBitmap(src, 0f, 0f, paint)
        src.recycle()
        src = null
        return bitmapResult
    }

    fun grayscale(src: Bitmap?): Bitmap {
        //Array to generate Gray-Scale image
        var src = src
        val GrayArray = floatArrayOf(
            0.213f, 0.715f, 0.072f, 0.0f, 0.0f,
            0.213f, 0.715f, 0.072f, 0.0f, 0.0f,
            0.213f, 0.715f, 0.072f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f, 0.0f
        )
        val colorMatrixGray = ColorMatrix(GrayArray)
        val w = src!!.width
        val h = src.height
        val bitmapResult = Bitmap
            .createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvasResult = Canvas(bitmapResult)
        val paint = Paint()
        val filter = ColorMatrixColorFilter(colorMatrixGray)
        paint.colorFilter = filter
        canvasResult.drawBitmap(src, 0f, 0f, paint)
        src.recycle()
        src = null
        return bitmapResult
    }

    fun vignette(image: Bitmap): Bitmap {
        val width = image.width
        val height = image.height
        val radius = (width / 1.2).toFloat()
        val colors = intArrayOf(0, 0x55000000, -0x1000000)
        val positions = floatArrayOf(0.0f, 0.5f, 1.0f)
        val gradient: RadialGradient =
            RadialGradient(
                (width / 2).toFloat(),
                (height / 2).toFloat(),
                radius,
                colors,
                positions,
                Shader.TileMode.CLAMP
            )

        //RadialGradient gradient = new RadialGradient(width / 2, height / 2, radius, Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP);
        val canvas = Canvas(image)
        canvas.drawARGB(1, 0, 0, 0)
        val paint = Paint()
        paint.isAntiAlias = true
        paint.color = Color.BLACK
        paint.shader = gradient
        val rect = Rect(0, 0, image.width, image.height)
        val rectf = RectF(rect)
        canvas.drawRect(rectf, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(image, rect, rect, paint)
        return image
    }

    // hue = [0, 360] -> Default = 0
    fun hue(bitmap: Bitmap?, hue: Float): Bitmap {
        var bitmap = bitmap
        val newBitmap = bitmap!!.copy(bitmap.config, true)
        val width = newBitmap.width
        val height = newBitmap.height
        val hsv = FloatArray(3)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = newBitmap.getPixel(x, y)
                Color.colorToHSV(pixel, hsv)
                hsv[0] = hue
                newBitmap.setPixel(x, y, Color.HSVToColor(Color.alpha(pixel), hsv))
            }
        }
        bitmap.recycle()
        bitmap = null
        return newBitmap
    }

    fun tint(src: Bitmap?, color: Int): Bitmap {
        // image size
        var src = src
        val width = src!!.width
        val height = src.height
        // create output bitmap

        // create a mutable empty bitmap
        val bmOut = Bitmap.createBitmap(width, height, src.config)
        val p = Paint(Color.RED)
        val filter: ColorFilter = LightingColorFilter(color, 1)
        p.colorFilter = filter
        val c = Canvas()
        c.setBitmap(bmOut)
        c.drawBitmap(src, 0f, 0f, p)
        src.recycle()
        src = null
        return bmOut
    }

    fun invert(src: Bitmap?): Bitmap {
        var src = src
        val output = Bitmap.createBitmap(src!!.width, src.height, src.config)
        var A: Int
        var R: Int
        var G: Int
        var B: Int
        var pixelColor: Int
        val height = src.height
        val width = src.width
        for (y in 0 until height) {
            for (x in 0 until width) {
                pixelColor = src.getPixel(x, y)
                A = Color.alpha(pixelColor)
                R = 255 - Color.red(pixelColor)
                G = 255 - Color.green(pixelColor)
                B = 255 - Color.blue(pixelColor)
                output.setPixel(x, y, Color.argb(A, R, G, B))
            }
        }
        src.recycle()
        src = null
        return output
    }

    // percent = [0, 150], type = (1, 2, 3) => (R, G, B)
    fun boost(src: Bitmap?, type: Int, percent: Float): Bitmap {
        var src = src
        var percent = percent
        percent = percent / 100
        val width = src!!.width
        val height = src.height
        val bmOut = Bitmap.createBitmap(width, height, src.config)
        var A: Int
        var R: Int
        var G: Int
        var B: Int
        var pixel: Int
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixel = src.getPixel(x, y)
                A = Color.alpha(pixel)
                R = Color.red(pixel)
                G = Color.green(pixel)
                B = Color.blue(pixel)
                if (type == 1) {
                    R = (R * (1 + percent)).toInt()
                    if (R > 255) R = 255
                } else if (type == 2) {
                    G = (G * (1 + percent)).toInt()
                    if (G > 255) G = 255
                } else if (type == 3) {
                    B = (B * (1 + percent)).toInt()
                    if (B > 255) B = 255
                }
                bmOut.setPixel(x, y, Color.argb(A, R, G, B))
            }
        }
        src.recycle()
        src = null
        return bmOut
    }

    fun sketch(src: Bitmap?): Bitmap {
        var src = src
        val type = 6
        val threshold = 130
        val width = src!!.width
        val height = src.height
        val result = Bitmap.createBitmap(width, height, src.config)
        var A: Int
        var R: Int
        var G: Int
        var B: Int
        var sumR: Int
        var sumG: Int
        var sumB: Int
        val pixels = Array(3) { IntArray(3) }
        for (y in 0 until height - 2) {
            for (x in 0 until width - 2) {
                //      get pixel matrix
                for (i in 0..2) {
                    for (j in 0..2) {
                        pixels[i][j] = src.getPixel(x + i, y + j)
                    }
                }
                // get alpha of center pixel
                A = Color.alpha(pixels[1][1])
                // init color sum
                sumB = 0
                sumG = sumB
                sumR = sumG
                sumR = type * Color.red(pixels[1][1]) - Color.red(
                    pixels[0][0]
                ) - Color.red(pixels[0][2]) - Color.red(pixels[2][0]) - Color.red(
                    pixels[2][2]
                )
                sumG = type * Color.green(pixels[1][1]) - Color.green(
                    pixels[0][0]
                ) - Color.green(pixels[0][2]) - Color.green(pixels[2][0]) - Color.green(
                    pixels[2][2]
                )
                sumB = type * Color.blue(pixels[1][1]) - Color.blue(
                    pixels[0][0]
                ) - Color.blue(pixels[0][2]) - Color.blue(pixels[2][0]) - Color.blue(
                    pixels[2][2]
                )
                // get final Red
                R = (sumR + threshold)
                if (R < 0) {
                    R = 0
                } else if (R > 255) {
                    R = 255
                }
                // get final Green
                G = (sumG + threshold)
                if (G < 0) {
                    G = 0
                } else if (G > 255) {
                    G = 255
                }
                // get final Blue
                B = (sumB + threshold)
                if (B < 0) {
                    B = 0
                } else if (B > 255) {
                    B = 255
                }
                result.setPixel(x + 1, y + 1, Color.argb(A, R, G, B))
            }
        }
        src.recycle()
        src = null
        return result
    }

    @Throws(IOException::class)
    fun modifyOrientation(bitmap: Bitmap, image_url: String?): Bitmap {
        val ei = ExifInterface(image_url!!)
        val orientation =
            ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotate(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotate(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotate(bitmap, 270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flip(bitmap, true, false)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> flip(bitmap, false, true)
            else -> bitmap
        }
    }
}