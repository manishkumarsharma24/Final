export default function SkeletonCard() {
  return (
    <div className="skeleton-card">
      <div className="skel skel-img" />
      <div className="skel-body">
        <div className="skel skel-line short" />
        <div className="skel skel-line" />
        <div className="skel skel-line medium" />
        <div className="skel-footer">
          <div className="skel skel-price" />
          <div className="skel skel-btn" />
        </div>
      </div>
    </div>
  );
}
