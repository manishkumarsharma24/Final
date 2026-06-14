import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';

const EMOJI = {
  electronics: '💻', furniture: '🪑', kitchen: '🍳',
  clothing: '👕', lifestyle: '🌿', accessories: '👜',
};

export default function ProductCard({ product, onAdd }) {
  const { addToCart, items } = useCart();
  const navigate = useNavigate();
  const [added, setAdded] = useState(false);

  const inCart = items.some(i => i.id === product.id);
  const outOfStock = product.stockQuantity === 0;
  const lowStock = product.stockQuantity > 0 && product.stockQuantity <= 5;

  const handleAdd = (e) => {
    e.stopPropagation();
    if (outOfStock) return;
    if (onAdd) {
      onAdd(product);
    } else {
      addToCart(product);
    }
    setAdded(true);
    setTimeout(() => setAdded(false), 2000);
  };

  return (
    <div className="product-card" onClick={() => navigate(`/products/${product.id}`)}>
      <div className="product-img-wrap">
        <span>{EMOJI[product.category] || '📦'}</span>
        {lowStock && <span className="product-badge badge-low">Low stock</span>}
        {outOfStock && <span className="product-badge badge-out">Out of stock</span>}
      </div>
      <div className="product-body">
        <div className="product-category">{product.category}</div>
        <div className="product-name">{product.name}</div>
        <div className="product-desc">{product.description}</div>
        <div className="product-price">${product.price?.toFixed(2)}</div>
        <button
          className={`product-card-btn ${added || inCart ? 'added' : ''}`}
          onClick={handleAdd}
          disabled={outOfStock}
        >
          {outOfStock ? 'Out of Stock' : added || inCart ? '✓ Added' : '+ Add to Cart'}
        </button>
      </div>
    </div>
  );
}
