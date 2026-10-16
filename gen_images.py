import os
from PIL import Image, ImageDraw

def create_banner_drama():
    img = Image.new('RGB', (800, 450), color='#b8860b') # Dark gold/warm tone
    draw = ImageDraw.Draw(img)
    # Draw a luxury living room vibe (just some geometric shapes)
    draw.rectangle([100, 100, 700, 350], fill='#8b6508')
    # Draw 3 silhouettes
    draw.ellipse([250, 150, 300, 200], fill='#333333')
    draw.ellipse([375, 150, 425, 200], fill='#333333')
    draw.ellipse([500, 150, 550, 200], fill='#333333')
    draw.rectangle([260, 200, 290, 350], fill='#333333')
    draw.rectangle([385, 200, 415, 350], fill='#333333')
    draw.rectangle([510, 200, 540, 350], fill='#333333')

    os.makedirs('app/src/main/res/drawable', exist_ok=True)
    img.save('app/src/main/res/drawable/banner_channel_drama.png')

def create_banner_all_genres():
    img = Image.new('RGB', (800, 450))
    draw = ImageDraw.Draw(img)
    # 3 vertical splits
    # Left: dark blue (mystery)
    draw.rectangle([0, 0, 266, 450], fill='#000033')
    # Center: warm orange (comedy)
    draw.rectangle([266, 0, 533, 450], fill='#ff8c00')
    # Right: rose gold (romantic)
    draw.rectangle([533, 0, 800, 450], fill='#b76e79')

    os.makedirs('app/src/main/res/drawable', exist_ok=True)
    img.save('app/src/main/res/drawable/banner_channel_all_genres.png')

create_banner_drama()
create_banner_all_genres()
