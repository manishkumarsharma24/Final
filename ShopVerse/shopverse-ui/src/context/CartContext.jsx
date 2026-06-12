import { createContext, useContext, useReducer } from 'react';

const CartContext = createContext(null);

function cartReducer(state, action) {
  switch (action.type) {
    case 'ADD': {
      const existing = state.find(i => i.id === action.product.id);
      if (existing) {
        return state.map(i => i.id === action.product.id
          ? { ...i, quantity: i.quantity + 1 } : i);
      }
      return [...state, { ...action.product, quantity: 1 }];
    }
    case 'REMOVE':
      return state.filter(i => i.id !== action.id);
    case 'UPDATE_QTY':
      if (action.qty <= 0) return state.filter(i => i.id !== action.id);
      return state.map(i => i.id === action.id ? { ...i, quantity: action.qty } : i);
    case 'CLEAR':
      return [];
    default:
      return state;
  }
}

export function CartProvider({ children }) {
  const [items, dispatch] = useReducer(cartReducer, []);

  const addToCart    = (product)       => dispatch({ type: 'ADD', product });
  const removeFromCart = (id)          => dispatch({ type: 'REMOVE', id });
  const updateQty    = (id, qty)       => dispatch({ type: 'UPDATE_QTY', id, qty });
  const clearCart    = ()              => dispatch({ type: 'CLEAR' });

  const totalItems   = items.reduce((s, i) => s + i.quantity, 0);
  const totalPrice   = items.reduce((s, i) => s + i.price * i.quantity, 0);

  return (
    <CartContext.Provider value={{ items, addToCart, removeFromCart, updateQty, clearCart, totalItems, totalPrice }}>
      {children}
    </CartContext.Provider>
  );
}

export const useCart = () => useContext(CartContext);
