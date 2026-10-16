import re

with open("app/src/main/java/com/pearparinya/directorai/MainActivity.kt", "r") as f:
    content = f.read()

colors_block_new = """
    private val navy = Color.parseColor("#0B0C1E")
    private val navyLight = Color.parseColor("#121024")
    private val headerBg = Color.parseColor("#1E1424")
    private val headerStroke = Color.parseColor("#8A6D3B")
    private val pillBg = Color.parseColor("#DCE4EC")
    private val pillText = Color.parseColor("#E5A93C")
    private val stepActive = Color.parseColor("#0088FF")
    private val stepInactive = Color.parseColor("#3A3F58")
    private val card1Bg = Color.parseColor("#122244")
    private val card1Border = Color.parseColor("#00A8FF")
    private val cardOtherBg = Color.parseColor("#181C26")
    private val gridBg = Color.parseColor("#14161F")
    private val gridBorder = Color.parseColor("#2E3446")
    private val gridTextGold = Color.parseColor("#F5B041")
    private val footerText = Color.parseColor("#9AA4B2")
    private val popupBg = Color.parseColor("#111526")
    private val panel = Color.rgb(37, 32, 78)
    private val gold = Color.parseColor("#F5B041")
    private val ice = Color.rgb(248, 247, 255)
    private val muted = Color.rgb(196, 193, 222)
    private val success = Color.rgb(83, 224, 210)
    private val activeGreen = Color.rgb(46, 204, 113)
    private val flowBlue = Color.rgb(41, 128, 185)
"""

content = re.sub(
    r'private val navy = Color\.rgb\(12, 15, 38\).*?private val flowBlue = Color\.rgb\(41, 128, 185\)',
    colors_block_new.strip(),
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/pearparinya/directorai/MainActivity.kt", "w") as f:
    f.write(content)
