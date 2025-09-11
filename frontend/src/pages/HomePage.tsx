import HomeCard from "../components/HomeCard";
import { mockData } from "../types/Kospi200_5years";

function HomePage() {
  return (
    <div className="grid grid-cols-3 gap-4 p-4  ">
      {mockData.map((item) => (
        <HomeCard key={item.id} {...item} />
      ))}
    </div>
  );
}

export default HomePage;
