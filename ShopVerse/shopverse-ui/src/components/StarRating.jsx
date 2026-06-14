import { useState } from 'react';

export default function StarRating({ value = 0, onChange, readOnly = false, size = 24 }) {
  const [hovered, setHovered] = useState(0);
  const display = hovered || value;

  return (
    <div className="star-rating" style={{ fontSize: size }}>
      {[1, 2, 3, 4, 5].map(star => (
        <span
          key={star}
          className={`star ${star <= display ? 'filled' : 'empty'}`}
          onClick={() => !readOnly && onChange?.(star)}
          onMouseEnter={() => !readOnly && setHovered(star)}
          onMouseLeave={() => !readOnly && setHovered(0)}
          style={{ cursor: readOnly ? 'default' : 'pointer' }}
        >
          {star <= display ? '★' : '☆'}
        </span>
      ))}
    </div>
  );
}
