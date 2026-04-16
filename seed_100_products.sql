-- Seed 100 real products (title + matching image URL) from DummyJSON catalog.
-- Safe to rerun: it skips products that already exist by name (case-insensitive).

DO $$
DECLARE
    category_table text;
    product_table text;
    product_table_name text;
    image_column text;
    inserted_count integer := 0;
BEGIN
    IF to_regclass('public."Category"') IS NOT NULL THEN
        category_table := '"Category"';
    ELSIF to_regclass('public.category') IS NOT NULL THEN
        category_table := 'category';
    ELSE
        RAISE EXCEPTION 'Category table was not found.';
    END IF;

    IF to_regclass('public."Product"') IS NOT NULL THEN
        product_table := '"Product"';
        product_table_name := 'Product';
    ELSIF to_regclass('public.product') IS NOT NULL THEN
        product_table := 'product';
        product_table_name := 'product';
    ELSE
        RAISE EXCEPTION 'Product table was not found.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = product_table_name
          AND column_name = 'imageUrl'
    ) THEN
        image_column := '"imageUrl"';
    ELSIF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = product_table_name
          AND column_name = 'image_url'
    ) THEN
        image_column := 'image_url';
    ELSE
        RAISE EXCEPTION 'Image column was not found on %.', product_table_name;
    END IF;

    CREATE TEMP TABLE seed_products (
        product_name text NOT NULL,
        category_name text NOT NULL,
        price numeric(10,2) NOT NULL,
        stock integer NOT NULL,
        image_url text NOT NULL
    ) ON COMMIT DROP;

    INSERT INTO seed_products (product_name, category_name, price, stock, image_url)
    VALUES
        ('Essence Mascara Lash Princess', 'Beauty', 9.99, 99, 'https://cdn.dummyjson.com/product-images/beauty/essence-mascara-lash-princess/thumbnail.webp'),
        ('Eyeshadow Palette with Mirror', 'Beauty', 19.99, 34, 'https://cdn.dummyjson.com/product-images/beauty/eyeshadow-palette-with-mirror/thumbnail.webp'),
        ('Powder Canister', 'Beauty', 14.99, 89, 'https://cdn.dummyjson.com/product-images/beauty/powder-canister/thumbnail.webp'),
        ('Red Lipstick', 'Beauty', 12.99, 91, 'https://cdn.dummyjson.com/product-images/beauty/red-lipstick/thumbnail.webp'),
        ('Red Nail Polish', 'Beauty', 8.99, 79, 'https://cdn.dummyjson.com/product-images/beauty/red-nail-polish/thumbnail.webp'),
        ('Calvin Klein CK One', 'Fragrances', 49.99, 29, 'https://cdn.dummyjson.com/product-images/fragrances/calvin-klein-ck-one/thumbnail.webp'),
        ('Chanel Coco Noir Eau De', 'Fragrances', 129.99, 58, 'https://cdn.dummyjson.com/product-images/fragrances/chanel-coco-noir-eau-de/thumbnail.webp'),
        ('Dior J''adore', 'Fragrances', 89.99, 98, 'https://cdn.dummyjson.com/product-images/fragrances/dior-j''adore/thumbnail.webp'),
        ('Dolce Shine Eau de', 'Fragrances', 69.99, 4, 'https://cdn.dummyjson.com/product-images/fragrances/dolce-shine-eau-de/thumbnail.webp'),
        ('Gucci Bloom Eau de', 'Fragrances', 79.99, 91, 'https://cdn.dummyjson.com/product-images/fragrances/gucci-bloom-eau-de/thumbnail.webp'),
        ('Annibale Colombo Bed', 'Furniture', 1899.99, 88, 'https://cdn.dummyjson.com/product-images/furniture/annibale-colombo-bed/thumbnail.webp'),
        ('Annibale Colombo Sofa', 'Furniture', 2499.99, 60, 'https://cdn.dummyjson.com/product-images/furniture/annibale-colombo-sofa/thumbnail.webp'),
        ('Bedside Table African Cherry', 'Furniture', 299.99, 64, 'https://cdn.dummyjson.com/product-images/furniture/bedside-table-african-cherry/thumbnail.webp'),
        ('Knoll Saarinen Executive Conference Chair', 'Furniture', 499.99, 26, 'https://cdn.dummyjson.com/product-images/furniture/knoll-saarinen-executive-conference-chair/thumbnail.webp'),
        ('Wooden Bathroom Sink With Mirror', 'Furniture', 799.99, 7, 'https://cdn.dummyjson.com/product-images/furniture/wooden-bathroom-sink-with-mirror/thumbnail.webp'),
        ('Apple', 'Groceries', 1.99, 8, 'https://cdn.dummyjson.com/product-images/groceries/apple/thumbnail.webp'),
        ('Beef Steak', 'Groceries', 12.99, 86, 'https://cdn.dummyjson.com/product-images/groceries/beef-steak/thumbnail.webp'),
        ('Cat Food', 'Groceries', 8.99, 46, 'https://cdn.dummyjson.com/product-images/groceries/cat-food/thumbnail.webp'),
        ('Chicken Meat', 'Groceries', 9.99, 97, 'https://cdn.dummyjson.com/product-images/groceries/chicken-meat/thumbnail.webp'),
        ('Cooking Oil', 'Groceries', 4.99, 10, 'https://cdn.dummyjson.com/product-images/groceries/cooking-oil/thumbnail.webp'),
        ('Cucumber', 'Groceries', 1.49, 84, 'https://cdn.dummyjson.com/product-images/groceries/cucumber/thumbnail.webp'),
        ('Dog Food', 'Groceries', 10.99, 71, 'https://cdn.dummyjson.com/product-images/groceries/dog-food/thumbnail.webp'),
        ('Eggs', 'Groceries', 2.99, 9, 'https://cdn.dummyjson.com/product-images/groceries/eggs/thumbnail.webp'),
        ('Fish Steak', 'Groceries', 14.99, 74, 'https://cdn.dummyjson.com/product-images/groceries/fish-steak/thumbnail.webp'),
        ('Green Bell Pepper', 'Groceries', 1.29, 33, 'https://cdn.dummyjson.com/product-images/groceries/green-bell-pepper/thumbnail.webp'),
        ('Green Chili Pepper', 'Groceries', 0.99, 3, 'https://cdn.dummyjson.com/product-images/groceries/green-chili-pepper/thumbnail.webp'),
        ('Honey Jar', 'Groceries', 6.99, 34, 'https://cdn.dummyjson.com/product-images/groceries/honey-jar/thumbnail.webp'),
        ('Ice Cream', 'Groceries', 5.49, 27, 'https://cdn.dummyjson.com/product-images/groceries/ice-cream/thumbnail.webp'),
        ('Juice', 'Groceries', 3.99, 50, 'https://cdn.dummyjson.com/product-images/groceries/juice/thumbnail.webp'),
        ('Kiwi', 'Groceries', 2.49, 99, 'https://cdn.dummyjson.com/product-images/groceries/kiwi/thumbnail.webp'),
        ('Lemon', 'Groceries', 0.79, 31, 'https://cdn.dummyjson.com/product-images/groceries/lemon/thumbnail.webp'),
        ('Milk', 'Groceries', 3.49, 27, 'https://cdn.dummyjson.com/product-images/groceries/milk/thumbnail.webp'),
        ('Mulberry', 'Groceries', 4.99, 99, 'https://cdn.dummyjson.com/product-images/groceries/mulberry/thumbnail.webp'),
        ('Nescafe Coffee', 'Groceries', 7.99, 57, 'https://cdn.dummyjson.com/product-images/groceries/nescafe-coffee/thumbnail.webp'),
        ('Potatoes', 'Groceries', 2.29, 13, 'https://cdn.dummyjson.com/product-images/groceries/potatoes/thumbnail.webp'),
        ('Protein Powder', 'Groceries', 19.99, 80, 'https://cdn.dummyjson.com/product-images/groceries/protein-powder/thumbnail.webp'),
        ('Red Onions', 'Groceries', 1.99, 82, 'https://cdn.dummyjson.com/product-images/groceries/red-onions/thumbnail.webp'),
        ('Rice', 'Groceries', 5.99, 59, 'https://cdn.dummyjson.com/product-images/groceries/rice/thumbnail.webp'),
        ('Soft Drinks', 'Groceries', 1.99, 53, 'https://cdn.dummyjson.com/product-images/groceries/soft-drinks/thumbnail.webp'),
        ('Strawberry', 'Groceries', 3.99, 46, 'https://cdn.dummyjson.com/product-images/groceries/strawberry/thumbnail.webp'),
        ('Tissue Paper Box', 'Groceries', 2.49, 86, 'https://cdn.dummyjson.com/product-images/groceries/tissue-paper-box/thumbnail.webp'),
        ('Water', 'Groceries', 0.99, 53, 'https://cdn.dummyjson.com/product-images/groceries/water/thumbnail.webp'),
        ('Decoration Swing', 'Home decoration', 59.99, 47, 'https://cdn.dummyjson.com/product-images/home-decoration/decoration-swing/thumbnail.webp'),
        ('Family Tree Photo Frame', 'Home decoration', 29.99, 77, 'https://cdn.dummyjson.com/product-images/home-decoration/family-tree-photo-frame/thumbnail.webp'),
        ('House Showpiece Plant', 'Home decoration', 39.99, 28, 'https://cdn.dummyjson.com/product-images/home-decoration/house-showpiece-plant/thumbnail.webp'),
        ('Plant Pot', 'Home decoration', 14.99, 59, 'https://cdn.dummyjson.com/product-images/home-decoration/plant-pot/thumbnail.webp'),
        ('Table Lamp', 'Home decoration', 49.99, 9, 'https://cdn.dummyjson.com/product-images/home-decoration/table-lamp/thumbnail.webp'),
        ('Bamboo Spatula', 'Kitchen accessories', 7.99, 37, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/bamboo-spatula/thumbnail.webp'),
        ('Black Aluminium Cup', 'Kitchen accessories', 5.99, 75, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/black-aluminium-cup/thumbnail.webp'),
        ('Black Whisk', 'Kitchen accessories', 9.99, 73, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/black-whisk/thumbnail.webp'),
        ('Boxed Blender', 'Kitchen accessories', 39.99, 9, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/boxed-blender/thumbnail.webp'),
        ('Carbon Steel Wok', 'Kitchen accessories', 29.99, 40, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/carbon-steel-wok/thumbnail.webp'),
        ('Chopping Board', 'Kitchen accessories', 12.99, 14, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/chopping-board/thumbnail.webp'),
        ('Citrus Squeezer Yellow', 'Kitchen accessories', 8.99, 22, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/citrus-squeezer-yellow/thumbnail.webp'),
        ('Egg Slicer', 'Kitchen accessories', 6.99, 40, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/egg-slicer/thumbnail.webp'),
        ('Electric Stove', 'Kitchen accessories', 49.99, 21, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/electric-stove/thumbnail.webp'),
        ('Fine Mesh Strainer', 'Kitchen accessories', 9.99, 85, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/fine-mesh-strainer/thumbnail.webp'),
        ('Fork', 'Kitchen accessories', 3.99, 7, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/fork/thumbnail.webp'),
        ('Glass', 'Kitchen accessories', 4.99, 46, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/glass/thumbnail.webp'),
        ('Grater Black', 'Kitchen accessories', 10.99, 84, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/grater-black/thumbnail.webp'),
        ('Hand Blender', 'Kitchen accessories', 34.99, 84, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/hand-blender/thumbnail.webp'),
        ('Ice Cube Tray', 'Kitchen accessories', 5.99, 13, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/ice-cube-tray/thumbnail.webp'),
        ('Kitchen Sieve', 'Kitchen accessories', 7.99, 68, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/kitchen-sieve/thumbnail.webp'),
        ('Knife', 'Kitchen accessories', 14.99, 7, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/knife/thumbnail.webp'),
        ('Lunch Box', 'Kitchen accessories', 12.99, 94, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/lunch-box/thumbnail.webp'),
        ('Microwave Oven', 'Kitchen accessories', 89.99, 59, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/microwave-oven/thumbnail.webp'),
        ('Mug Tree Stand', 'Kitchen accessories', 15.99, 88, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/mug-tree-stand/thumbnail.webp'),
        ('Pan', 'Kitchen accessories', 24.99, 90, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/pan/thumbnail.webp'),
        ('Plate', 'Kitchen accessories', 3.99, 66, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/plate/thumbnail.webp'),
        ('Red Tongs', 'Kitchen accessories', 6.99, 82, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/red-tongs/thumbnail.webp'),
        ('Silver Pot With Glass Cap', 'Kitchen accessories', 39.99, 40, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/silver-pot-with-glass-cap/thumbnail.webp'),
        ('Slotted Turner', 'Kitchen accessories', 8.99, 88, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/slotted-turner/thumbnail.webp'),
        ('Spice Rack', 'Kitchen accessories', 19.99, 79, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/spice-rack/thumbnail.webp'),
        ('Spoon', 'Kitchen accessories', 4.99, 59, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/spoon/thumbnail.webp'),
        ('Tray', 'Kitchen accessories', 16.99, 71, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/tray/thumbnail.webp'),
        ('Wooden Rolling Pin', 'Kitchen accessories', 11.99, 80, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/wooden-rolling-pin/thumbnail.webp'),
        ('Yellow Peeler', 'Kitchen accessories', 5.99, 35, 'https://cdn.dummyjson.com/product-images/kitchen-accessories/yellow-peeler/thumbnail.webp'),
        ('Apple MacBook Pro 14 Inch Space Grey', 'Laptops', 1999.99, 24, 'https://cdn.dummyjson.com/product-images/laptops/apple-macbook-pro-14-inch-space-grey/thumbnail.webp'),
        ('Asus Zenbook Pro Dual Screen Laptop', 'Laptops', 1799.99, 45, 'https://cdn.dummyjson.com/product-images/laptops/asus-zenbook-pro-dual-screen-laptop/thumbnail.webp'),
        ('Huawei Matebook X Pro', 'Laptops', 1399.99, 75, 'https://cdn.dummyjson.com/product-images/laptops/huawei-matebook-x-pro/thumbnail.webp'),
        ('Lenovo Yoga 920', 'Laptops', 1099.99, 40, 'https://cdn.dummyjson.com/product-images/laptops/lenovo-yoga-920/thumbnail.webp'),
        ('New DELL XPS 13 9300 Laptop', 'Laptops', 1499.99, 74, 'https://cdn.dummyjson.com/product-images/laptops/new-dell-xps-13-9300-laptop/thumbnail.webp'),
        ('Blue & Black Check Shirt', 'Mens shirts', 29.99, 38, 'https://cdn.dummyjson.com/product-images/mens-shirts/blue-&-black-check-shirt/thumbnail.webp'),
        ('Gigabyte Aorus Men Tshirt', 'Mens shirts', 24.99, 90, 'https://cdn.dummyjson.com/product-images/mens-shirts/gigabyte-aorus-men-tshirt/thumbnail.webp'),
        ('Man Plaid Shirt', 'Mens shirts', 34.99, 82, 'https://cdn.dummyjson.com/product-images/mens-shirts/man-plaid-shirt/thumbnail.webp'),
        ('Man Short Sleeve Shirt', 'Mens shirts', 19.99, 2, 'https://cdn.dummyjson.com/product-images/mens-shirts/man-short-sleeve-shirt/thumbnail.webp'),
        ('Men Check Shirt', 'Mens shirts', 27.99, 95, 'https://cdn.dummyjson.com/product-images/mens-shirts/men-check-shirt/thumbnail.webp'),
        ('Nike Air Jordan 1 Red And Black', 'Mens shoes', 149.99, 7, 'https://cdn.dummyjson.com/product-images/mens-shoes/nike-air-jordan-1-red-and-black/thumbnail.webp'),
        ('Nike Baseball Cleats', 'Mens shoes', 79.99, 12, 'https://cdn.dummyjson.com/product-images/mens-shoes/nike-baseball-cleats/thumbnail.webp'),
        ('Puma Future Rider Trainers', 'Mens shoes', 89.99, 90, 'https://cdn.dummyjson.com/product-images/mens-shoes/puma-future-rider-trainers/thumbnail.webp'),
        ('Sports Sneakers Off White & Red', 'Mens shoes', 119.99, 17, 'https://cdn.dummyjson.com/product-images/mens-shoes/sports-sneakers-off-white-&-red/thumbnail.webp'),
        ('Sports Sneakers Off White Red', 'Mens shoes', 109.99, 62, 'https://cdn.dummyjson.com/product-images/mens-shoes/sports-sneakers-off-white-red/thumbnail.webp'),
        ('Brown Leather Belt Watch', 'Mens watches', 89.99, 32, 'https://cdn.dummyjson.com/product-images/mens-watches/brown-leather-belt-watch/thumbnail.webp'),
        ('Longines Master Collection', 'Mens watches', 1499.99, 100, 'https://cdn.dummyjson.com/product-images/mens-watches/longines-master-collection/thumbnail.webp'),
        ('Rolex Cellini Date Black Dial', 'Mens watches', 8999.99, 40, 'https://cdn.dummyjson.com/product-images/mens-watches/rolex-cellini-date-black-dial/thumbnail.webp'),
        ('Rolex Cellini Moonphase', 'Mens watches', 12999.99, 36, 'https://cdn.dummyjson.com/product-images/mens-watches/rolex-cellini-moonphase/thumbnail.webp'),
        ('Rolex Datejust', 'Mens watches', 10999.99, 86, 'https://cdn.dummyjson.com/product-images/mens-watches/rolex-datejust/thumbnail.webp'),
        ('Rolex Submariner Watch', 'Mens watches', 13999.99, 55, 'https://cdn.dummyjson.com/product-images/mens-watches/rolex-submariner-watch/thumbnail.webp'),
        ('Amazon Echo Plus', 'Mobile accessories', 99.99, 61, 'https://cdn.dummyjson.com/product-images/mobile-accessories/amazon-echo-plus/thumbnail.webp'),
        ('Apple Airpods', 'Mobile accessories', 129.99, 67, 'https://cdn.dummyjson.com/product-images/mobile-accessories/apple-airpods/thumbnail.webp');

    EXECUTE format(
        'INSERT INTO %s ("name")
         SELECT DISTINCT s.category_name
         FROM seed_products s
         WHERE NOT EXISTS (
             SELECT 1
             FROM %s c
             WHERE lower(c."name") = lower(s.category_name)
         )',
        category_table,
        category_table
    );

    EXECUTE format(
        'INSERT INTO %s ("name", "price", "stock", %s, "category_id", "deleted")
         SELECT s.product_name, s.price, s.stock, s.image_url, c."id", false
         FROM seed_products s
         JOIN %s c
           ON lower(c."name") = lower(s.category_name)
         WHERE NOT EXISTS (
             SELECT 1
             FROM %s p
             WHERE lower(p."name") = lower(s.product_name)
         )',
        product_table,
        image_column,
        category_table,
        product_table
    );

    GET DIAGNOSTICS inserted_count = ROW_COUNT;
    RAISE NOTICE 'Seed completed. Inserted % products.', inserted_count;
END $$;
