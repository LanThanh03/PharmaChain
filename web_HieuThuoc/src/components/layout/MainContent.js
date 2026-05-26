import React from 'react';
import './MainContent.css';

function MainContent({ children }) {
  return (
    <main className="main-content">
      <div className="content-container">
        {children}
      </div>
    </main>
  );
}

export default MainContent;


