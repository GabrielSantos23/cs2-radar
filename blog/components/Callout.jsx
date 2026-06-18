import React from 'react';

export function Callout({ type = 'info', children }) {
  const isWarning = type === 'warning';
  const styles = {
    padding: '1rem 1.25rem',
    margin: '1.5rem 0',
    borderRadius: '0.375rem',
    borderLeft: '4px solid',
    backgroundColor: isWarning ? '#2a200c' : '#0d1b2a',
    borderColor: isWarning ? '#eab308' : '#3b82f6',
    color: isWarning ? '#fef08a' : '#dbeafe',
    fontSize: '0.95rem',
    lineHeight: '1.6',
  };

  return (
    <div style={styles}>
      {children}
    </div>
  );
}
