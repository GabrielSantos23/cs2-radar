import React, { useState } from 'react';

export function ImagePlaceholder({ src, alt, caption }) {
  const [hasError, setHasError] = useState(!src);

  const containerStyle = {
    margin: '2rem 0',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    width: '100%',
  };

  const placeholderStyle = {
    width: '100%',
    aspectRatio: '16/9',
    backgroundColor: '#1f2937',
    border: '1px dashed #4b5563',
    borderRadius: '0.5rem',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: '#9ca3af',
    fontSize: '0.875rem',
    fontWeight: '500',
    padding: '1rem',
    textAlign: 'center',
    boxSizing: 'border-box',
  };

  const imgStyle = {
    maxWidth: '100%',
    height: 'auto',
    borderRadius: '0.5rem',
    border: '1px solid #374151',
  };

  const captionStyle = {
    marginTop: '0.75rem',
    fontSize: '0.875rem',
    color: '#9ca3af',
    textAlign: 'center',
    fontStyle: 'italic',
  };

  return (
    <figure style={containerStyle}>
      {hasError ? (
        <div style={placeholderStyle}>
          <span>{alt || 'Imagem não disponível'}</span>
        </div>
      ) : (
        <img
          src={src}
          alt={alt}
          style={imgStyle}
          onError={() => setHasError(true)}
        />
      )}
      {caption && <figcaption style={captionStyle}>{caption}</figcaption>}
    </figure>
  );
}
