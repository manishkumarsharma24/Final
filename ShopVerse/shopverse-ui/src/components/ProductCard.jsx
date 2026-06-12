import { Link } from 'react-router-dom';
import { useCart } from '../context/CartContext';

const CATEGORY_EMOJI = {
  electronics: '⚡', furniture: '🪑', lifestyle: '✨',
  accessories: '👜', kitchen: '🍳', clothing: '👕', default: '📦',
};

export default function ProductCard({ product }) {
  const { addToCart, items } = useCart();
  const inCart = items.some(i => i.id === product.id);

  return (
    <div className="product-card">
      <div className="product-img-wrapper">
        <div className="product-img-placeholder">
          <span className="product-emoji">
            {CATEGORY_EMOJI[product.category] || CATEGORY_EMOJI.default}
          </span>
        </div>
        {product.stockQuantity < 10 && product.stockQuantity > 0 && (
          <span className="badge-low-stock">Only {product.stockQuantity} left</span>
        )}
        {product.stockQuantity === 0 && (
          <span className="badge-out-of-stock">Out of Stock</span>
        )}
      </div>

      <div className="product-info">
        <span className="product-category">{product.category}</span>
        <Link to={`/products/${product.id}`} className="product-name">{product.name}</Link>
        <p className="product-desc">{product.description?.slice(0, 80)}…</p>

        <div className="product-footer">
          <span className="product-price">${product.price.toFixed(2)}</span>
          <button
            className={inCart ? 'btn-in-cart' : 'btn-add-cart'}
            onClick={() => addToCart(product)}
            disabled={product.stockQuantity === 0}
          >
            {inCart ? '✓ In Cart' : '+ Add to Cart'}
          </button>
        </div>
      </div>
    </div>
  );
}
