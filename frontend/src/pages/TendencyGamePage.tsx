import React from 'react';

function TendencyGamePage(): JSX.Element {
    const data = "zz";
    return (
        <>
            <div>
                <PriceComponent dataSet={data} />
            </div>

            <div>
                <News />
                <TendencyGameComponent />
            </div>
        </>
    );
}

export default TendencyGamePage;




--------
    function PriceComponent(dataSet: string) {
        console.log(dataSet);
        return <div>Price Component</div>;
    }

function News() {
    return <div>News Component</div>;
}

function TendencyGameComponent() {
    return (
        <>
            <매수Component />
            <매수 & 매도 내역 />
        </>
    );
}

fucntion 매수Component() {
    return 
    <div style={{ border: '1px solid black', padding: '10px', margin: '10px' }}>
        <div>
            <myInfo />
            <price />
            <tags />
        </div>
        <div>
            <button > 매수Component </button>
            <button > 매도Component </button>
        </div>
        <div>
            <button > 다음주 </button>
            <button > 게임 종료 </button>
        </div>
    </div>;
}